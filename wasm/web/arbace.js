// arbace.js: Arbace's shim for the browser (doc/wasm/WEB.md; doc/wasm/WASM-SURVEY.md §9.2, WAS15 (a), WAS16 (a)).
//
// Part 1, the gojs imports and the Go class, is derived from Go's lib/wasm/wasm_exec.js at the pinned TamaGo go1.27.1
// (/root/tamago-go/lib/wasm/wasm_exec.js, 575 lines; Copyright 2018 The Go Authors, BSD-3-Clause, LICENSE.md's Go section):
// its 24 imports kept as they are (bin/wai js shim-check compares their names with Go's //go:wasmimport gojs directives, with
// wasm_exec.js's and with wai's headless world's), its fs and process placeholders replaced by part 2. The other parts are
// Arbace's own: 2 the file system (Node's callback API over memory, standard input from the page's input line), 3 the frame
// loop, 4 the input ring, 5 the WebGPU bridge (the command buffer of doc/wasm/WEB.md §2), 6 the start. No build step, no npm.
"use strict";

const consoleEl = document.getElementById("console");
const lineEl = document.getElementById("line");
const encoder = new TextEncoder("utf-8");
const decoder = new TextDecoder("utf-8");

function print(text) {
	consoleEl.textContent += text;
	consoleEl.scrollTop = consoleEl.scrollHeight;
}

// ---- 2. the file system: Node's callback API (as syscall/fs_js.go calls it) over a memory tree --------------------------------
const enoent = (p) => { const e = new Error("ENOENT: " + p); e.code = "ENOENT"; return e; };
const enosys = () => { const e = new Error("not implemented"); e.code = "ENOSYS"; return e; };
const files = new Map([["/", { dir: true, mode: 0o40755, mtime: Date.now() }], ["/tmp", { dir: true, mode: 0o41777, mtime: Date.now() }]]);
const fds = new Map(); // fd -> { path, pos, flags }
let nextFd = 3;
const stdinLines = []; // the input line's text, waiting for a read
const stdinWaiters = [];
const outBufs = { 1: "", 2: "" };

function norm(p) {
	const out = [];
	for (const c of (p.startsWith("/") ? p : globalThis.process.cwd() + "/" + p).split("/")) {
		if (c === "" || c === ".") continue;
		if (c === "..") out.pop(); else out.push(c);
	}
	return "/" + out.join("/");
}
function parent(p) { const i = p.lastIndexOf("/"); return i <= 0 ? "/" : p.slice(0, i); }
function stats(f) {
	const mode = f.dir ? (f.mode | 0o40000) : (f.mode | 0o100000);
	const size = f.dir ? 0 : f.data.length;
	return { dev: 0, ino: 0, mode, nlink: 1, uid: 0, gid: 0, rdev: 0, size, blksize: 4096, blocks: Math.ceil(size / 512),
		atimeMs: f.mtime, mtimeMs: f.mtime, ctimeMs: f.mtime, isDirectory: () => !!f.dir };
}
function writeOut(fd, buf) {
	outBufs[fd] += decoder.decode(buf);
	const nl = outBufs[fd].lastIndexOf("\n");
	if (nl !== -1) { print(outBufs[fd].substring(0, nl + 1)); outBufs[fd] = outBufs[fd].substring(nl + 1); }
	return buf.length;
}
const O = { O_WRONLY: 1, O_RDWR: 2, O_CREAT: 64, O_EXCL: 128, O_TRUNC: 512, O_APPEND: 1024, O_DIRECTORY: 65536 };
globalThis.fs = {
	constants: O,
	writeSync(fd, buf) { if (fd === 1 || fd === 2) return writeOut(fd, buf); throw enosys(); },
	write(fd, buf, offset, length, position, cb) {
		if (fd === 1 || fd === 2) { cb(null, writeOut(fd, buf.subarray(offset, offset + length))); return; }
		const d = fds.get(fd); if (!d) { const e = new Error("EBADF"); e.code = "EBADF"; cb(e); return; }
		const f = files.get(d.path);
		let pos = position !== null ? position : (d.flags & O.O_APPEND ? f.data.length : d.pos);
		const need = pos + length;
		if (need > f.data.length) { const n = new Uint8Array(need); n.set(f.data); f.data = n; }
		f.data.set(buf.subarray(offset, offset + length), pos);
		if (position === null) d.pos = pos + length;
		f.mtime = Date.now();
		cb(null, length);
	},
	read(fd, buf, offset, length, position, cb) {
		if (fd === 0) {
			const give = () => { const b = stdinLines.shift(); const n = Math.min(length, b.length); buf.set(b.subarray(0, n), offset);
				if (n < b.length) stdinLines.unshift(b.subarray(n)); cb(null, n); };
			if (stdinLines.length > 0) give(); else stdinWaiters.push(give);
			return;
		}
		const d = fds.get(fd); if (!d) { const e = new Error("EBADF"); e.code = "EBADF"; cb(e); return; }
		const f = files.get(d.path);
		const pos = position !== null ? position : d.pos;
		const n = Math.max(0, Math.min(length, f.data.length - pos));
		buf.set(f.data.subarray(pos, pos + n), offset);
		if (position === null) d.pos += n;
		cb(null, n);
	},
	open(path, flags, mode, cb) {
		const p = norm(path);
		let f = files.get(p);
		if (!f) {
			if (!(flags & O.O_CREAT)) { cb(enoent(p)); return; }
			const dir = files.get(parent(p)); if (!dir || !dir.dir) { cb(enoent(p)); return; }
			f = { dir: false, mode: mode & 0o777, data: new Uint8Array(0), mtime: Date.now() };
			files.set(p, f);
		} else if ((flags & O.O_CREAT) && (flags & O.O_EXCL)) { const e = new Error("EEXIST: " + p); e.code = "EEXIST"; cb(e); return; }
		if ((flags & O.O_TRUNC) && !f.dir) f.data = new Uint8Array(0);
		const fd = nextFd++;
		fds.set(fd, { path: p, pos: 0, flags });
		cb(null, fd);
	},
	close(fd, cb) { fds.delete(fd); cb(null); },
	fstat(fd, cb) {
		if (fd <= 2) { cb(null, { ...stats({ dir: false, mode: 0o20620, data: new Uint8Array(0), mtime: 0 }), mode: 0o20620 }); return; }
		const d = fds.get(fd); if (!d) { cb(enoent("fd " + fd)); return; }
		cb(null, stats(files.get(d.path)));
	},
	stat(path, cb) { const f = files.get(norm(path)); if (!f) cb(enoent(path)); else cb(null, stats(f)); },
	lstat(path, cb) { globalThis.fs.stat(path, cb); },
	readdir(path, cb) {
		const p = norm(path), pre = p === "/" ? "/" : p + "/";
		if (!files.get(p)) { cb(enoent(p)); return; }
		const names = [];
		for (const k of files.keys()) if (k !== p && k.startsWith(pre) && !k.slice(pre.length).includes("/")) names.push(k.slice(pre.length));
		cb(null, names.sort());
	},
	mkdir(path, perm, cb) {
		const p = norm(path);
		if (files.has(p)) { const e = new Error("EEXIST: " + p); e.code = "EEXIST"; cb(e); return; }
		files.set(p, { dir: true, mode: perm & 0o777, mtime: Date.now() }); cb(null);
	},
	unlink(path, cb) { const p = norm(path); if (!files.delete(p)) cb(enoent(p)); else cb(null); },
	rmdir(path, cb) { const p = norm(path); if (!files.delete(p)) cb(enoent(p)); else cb(null); },
	rename(from, to, cb) { const a = norm(from), f = files.get(a); if (!f) { cb(enoent(a)); return; } files.delete(a); files.set(norm(to), f); cb(null); },
	ftruncate(fd, length, cb) { const d = fds.get(fd); const f = files.get(d.path); const n = new Uint8Array(length); n.set(f.data.subarray(0, length)); f.data = n; cb(null); },
	truncate(path, length, cb) { const f = files.get(norm(path)); if (!f) { cb(enoent(path)); return; } const n = new Uint8Array(length); n.set(f.data.subarray(0, length)); f.data = n; cb(null); },
	fsync(fd, cb) { cb(null); },
	chmod(path, mode, cb) { cb(null); }, fchmod(fd, mode, cb) { cb(null); },
	chown(path, uid, gid, cb) { cb(null); }, fchown(fd, uid, gid, cb) { cb(null); }, lchown(path, uid, gid, cb) { cb(null); },
	utimes(path, atime, mtime, cb) { cb(null); },
	link(path, link, cb) { cb(enosys()); }, symlink(path, link, cb) { cb(enosys()); }, readlink(path, cb) { cb(enosys()); },
};
let cwd = "/";
globalThis.process = {
	getuid() { return -1; }, getgid() { return -1; }, geteuid() { return -1; }, getegid() { return -1; },
	getgroups() { throw enosys(); }, pid: -1, ppid: -1, umask() { throw enosys(); },
	cwd() { return cwd; }, chdir(p) { const q = norm(p); if (!files.get(q)) throw enoent(q); cwd = q; },
};
globalThis.path = { resolve(...segs) { let p = cwd; for (const s of segs) p = s.startsWith("/") ? s : p + "/" + s; return norm(p); } };

lineEl.addEventListener("keydown", (e) => {
	if (e.key !== "Enter") return;
	const text = lineEl.value + "\n";
	lineEl.value = "";
	print(text);
	stdinLines.push(encoder.encode(text));
	while (stdinWaiters.length > 0 && stdinLines.length > 0) stdinWaiters.shift()();
});

// ---- 1. the gojs imports (wasm_exec.js's, go1.27.1) --------------------------------------------------------------------------
class Go {
	constructor() {
		this.argv = ["js"];
		this.env = {};
		this.exit = (code) => { if (code !== 0) print("exit code: " + code + "\n"); };
		this._exitPromise = new Promise((resolve) => { this._resolveExitPromise = resolve; });
		this._pendingEvent = null;
		this._scheduledTimeouts = new Map();
		this._nextCallbackTimeoutID = 1;

		const setInt64 = (addr, v) => {
			this.mem.setUint32(addr + 0, v, true);
			this.mem.setUint32(addr + 4, Math.floor(v / 4294967296), true);
		};
		const setInt32 = (addr, v) => { this.mem.setUint32(addr + 0, v, true); };
		const getInt64 = (addr) => {
			const low = this.mem.getUint32(addr + 0, true);
			const high = this.mem.getInt32(addr + 4, true);
			return low + high * 4294967296;
		};
		const loadValue = (addr) => {
			const f = this.mem.getFloat64(addr, true);
			if (f === 0) return undefined;
			if (!isNaN(f)) return f;
			const id = this.mem.getUint32(addr, true);
			return this._values[id];
		};
		const storeValue = (addr, v) => {
			const nanHead = 0x7FF80000;
			if (typeof v === "number" && v !== 0) {
				if (isNaN(v)) {
					this.mem.setUint32(addr + 4, nanHead, true);
					this.mem.setUint32(addr, 0, true);
					return;
				}
				this.mem.setFloat64(addr, v, true);
				return;
			}
			if (v === undefined) { this.mem.setFloat64(addr, 0, true); return; }
			let id = this._ids.get(v);
			if (id === undefined) {
				id = this._idPool.pop();
				if (id === undefined) id = this._values.length;
				this._values[id] = v;
				this._goRefCounts[id] = 0;
				this._ids.set(v, id);
			}
			this._goRefCounts[id]++;
			let typeFlag = 0;
			switch (typeof v) {
				case "object": if (v !== null) typeFlag = 1; break;
				case "string": typeFlag = 2; break;
				case "symbol": typeFlag = 3; break;
				case "function": typeFlag = 4; break;
			}
			this.mem.setUint32(addr + 4, nanHead | typeFlag, true);
			this.mem.setUint32(addr, id, true);
		};
		const loadSlice = (addr) => {
			const array = getInt64(addr + 0);
			const len = getInt64(addr + 8);
			return new Uint8Array(this._inst.exports.mem.buffer, array, len);
		};
		const loadSliceOfValues = (addr) => {
			const array = getInt64(addr + 0);
			const len = getInt64(addr + 8);
			const a = new Array(len);
			for (let i = 0; i < len; i++) a[i] = loadValue(array + i * 8);
			return a;
		};
		const loadString = (addr) => {
			const saddr = getInt64(addr + 0);
			const len = getInt64(addr + 8);
			return decoder.decode(new DataView(this._inst.exports.mem.buffer, saddr, len));
		};
		const timeOrigin = Date.now() - performance.now();
		this.importObject = {
			gojs: {
				"runtime.wasmExit": (sp) => {
					sp >>>= 0;
					const code = this.mem.getInt32(sp + 8, true);
					this.exited = true;
					delete this._inst; delete this._values; delete this._goRefCounts; delete this._ids; delete this._idPool;
					this.exit(code);
				},
				"runtime.wasmWrite": (sp) => {
					sp >>>= 0;
					const fd = getInt64(sp + 8);
					const p = getInt64(sp + 16);
					const n = this.mem.getInt32(sp + 24, true);
					globalThis.fs.writeSync(fd, new Uint8Array(this._inst.exports.mem.buffer, p, n));
				},
				"runtime.resetMemoryDataView": (sp) => { sp >>>= 0; this.mem = new DataView(this._inst.exports.mem.buffer); },
				"runtime.nanotime1": (sp) => { sp >>>= 0; setInt64(sp + 8, (timeOrigin + performance.now()) * 1000000); },
				"runtime.walltime": (sp) => {
					sp >>>= 0;
					const msec = (new Date).getTime();
					setInt64(sp + 8, msec / 1000);
					this.mem.setInt32(sp + 16, (msec % 1000) * 1000000, true);
				},
				"runtime.scheduleTimeoutEvent": (sp) => {
					sp >>>= 0;
					const id = this._nextCallbackTimeoutID;
					this._nextCallbackTimeoutID++;
					this._scheduledTimeouts.set(id, setTimeout(() => {
						this._resume();
						while (this._scheduledTimeouts.has(id)) {
							// for some reason Go failed to register the timeout event, log and try again
							// (temporary workaround for https://github.com/golang/go/issues/28975)
							console.warn("scheduleTimeoutEvent: missed timeout event");
							this._resume();
						}
					}, getInt64(sp + 8)));
					this.mem.setInt32(sp + 16, id, true);
				},
				"runtime.clearTimeoutEvent": (sp) => {
					sp >>>= 0;
					const id = this.mem.getInt32(sp + 8, true);
					clearTimeout(this._scheduledTimeouts.get(id));
					this._scheduledTimeouts.delete(id);
				},
				"runtime.getRandomData": (sp) => { sp >>>= 0; crypto.getRandomValues(loadSlice(sp + 8)); },
				"syscall/js.finalizeRef": (sp) => {
					sp >>>= 0;
					const id = this.mem.getUint32(sp + 8, true);
					this._goRefCounts[id]--;
					if (this._goRefCounts[id] === 0) {
						const v = this._values[id];
						this._values[id] = null;
						this._ids.delete(v);
						this._idPool.push(id);
					}
				},
				"syscall/js.stringVal": (sp) => { sp >>>= 0; storeValue(sp + 24, loadString(sp + 8)); },
				"syscall/js.valueGet": (sp) => {
					sp >>>= 0;
					const result = Reflect.get(loadValue(sp + 8), loadString(sp + 16));
					sp = this._inst.exports.getsp() >>> 0;
					storeValue(sp + 32, result);
				},
				"syscall/js.valueSet": (sp) => { sp >>>= 0; Reflect.set(loadValue(sp + 8), loadString(sp + 16), loadValue(sp + 32)); },
				"syscall/js.valueDelete": (sp) => { sp >>>= 0; Reflect.deleteProperty(loadValue(sp + 8), loadString(sp + 16)); },
				"syscall/js.valueIndex": (sp) => { sp >>>= 0; storeValue(sp + 24, Reflect.get(loadValue(sp + 8), getInt64(sp + 16))); },
				"syscall/js.valueSetIndex": (sp) => { sp >>>= 0; Reflect.set(loadValue(sp + 8), getInt64(sp + 16), loadValue(sp + 24)); },
				"syscall/js.valueCall": (sp) => {
					sp >>>= 0;
					try {
						const v = loadValue(sp + 8);
						const m = Reflect.get(v, loadString(sp + 16));
						const args = loadSliceOfValues(sp + 32);
						const result = Reflect.apply(m, v, args);
						sp = this._inst.exports.getsp() >>> 0;
						storeValue(sp + 56, result);
						this.mem.setUint8(sp + 64, 1);
					} catch (err) {
						sp = this._inst.exports.getsp() >>> 0;
						storeValue(sp + 56, err);
						this.mem.setUint8(sp + 64, 0);
					}
				},
				"syscall/js.valueInvoke": (sp) => {
					sp >>>= 0;
					try {
						const v = loadValue(sp + 8);
						const args = loadSliceOfValues(sp + 16);
						const result = Reflect.apply(v, undefined, args);
						sp = this._inst.exports.getsp() >>> 0;
						storeValue(sp + 40, result);
						this.mem.setUint8(sp + 48, 1);
					} catch (err) {
						sp = this._inst.exports.getsp() >>> 0;
						storeValue(sp + 40, err);
						this.mem.setUint8(sp + 48, 0);
					}
				},
				"syscall/js.valueNew": (sp) => {
					sp >>>= 0;
					try {
						const v = loadValue(sp + 8);
						const args = loadSliceOfValues(sp + 16);
						const result = Reflect.construct(v, args);
						sp = this._inst.exports.getsp() >>> 0;
						storeValue(sp + 40, result);
						this.mem.setUint8(sp + 48, 1);
					} catch (err) {
						sp = this._inst.exports.getsp() >>> 0;
						storeValue(sp + 40, err);
						this.mem.setUint8(sp + 48, 0);
					}
				},
				"syscall/js.valueLength": (sp) => { sp >>>= 0; setInt64(sp + 16, parseInt(loadValue(sp + 8).length)); },
				"syscall/js.valuePrepareString": (sp) => {
					sp >>>= 0;
					const str = encoder.encode(String(loadValue(sp + 8)));
					storeValue(sp + 16, str);
					setInt64(sp + 24, str.length);
				},
				"syscall/js.valueLoadString": (sp) => { sp >>>= 0; const str = loadValue(sp + 8); loadSlice(sp + 16).set(str); },
				"syscall/js.valueInstanceOf": (sp) => {
					sp >>>= 0;
					this.mem.setUint8(sp + 24, (loadValue(sp + 8) instanceof loadValue(sp + 16)) ? 1 : 0);
				},
				"syscall/js.copyBytesToGo": (sp) => {
					sp >>>= 0;
					const dst = loadSlice(sp + 8);
					const src = loadValue(sp + 32);
					if (!(src instanceof Uint8Array || src instanceof Uint8ClampedArray)) { this.mem.setUint8(sp + 48, 0); return; }
					const toCopy = src.subarray(0, dst.length);
					dst.set(toCopy);
					setInt64(sp + 40, toCopy.length);
					this.mem.setUint8(sp + 48, 1);
				},
				"syscall/js.copyBytesToJS": (sp) => {
					sp >>>= 0;
					const dst = loadValue(sp + 8);
					const src = loadSlice(sp + 16);
					if (!(dst instanceof Uint8Array || dst instanceof Uint8ClampedArray)) { this.mem.setUint8(sp + 48, 0); return; }
					const toCopy = src.subarray(0, dst.length);
					dst.set(toCopy);
					setInt64(sp + 40, toCopy.length);
					this.mem.setUint8(sp + 48, 1);
				},
				"debug": (value) => { console.log(value); },
			},
		};
	}

	async run(instance) {
		this._inst = instance;
		this.mem = new DataView(this._inst.exports.mem.buffer);
		this._values = [NaN, 0, null, true, false, globalThis, this];
		this._goRefCounts = new Array(this._values.length).fill(Infinity);
		this._ids = new Map([[0, 1], [null, 2], [true, 3], [false, 4], [globalThis, 5], [this, 6]]);
		this._idPool = [];
		this.exited = false;
		let offset = 4096;
		const strPtr = (str) => {
			const ptr = offset;
			const bytes = encoder.encode(str + "\0");
			new Uint8Array(this.mem.buffer, offset, bytes.length).set(bytes);
			offset += bytes.length;
			if (offset % 8 !== 0) offset += 8 - (offset % 8);
			return ptr;
		};
		const argc = this.argv.length;
		const argvPtrs = [];
		this.argv.forEach((arg) => { argvPtrs.push(strPtr(arg)); });
		argvPtrs.push(0);
		const keys = Object.keys(this.env).sort();
		keys.forEach((key) => { argvPtrs.push(strPtr(`${key}=${this.env[key]}`)); });
		argvPtrs.push(0);
		const argv = offset;
		argvPtrs.forEach((ptr) => { this.mem.setUint32(offset, ptr, true); this.mem.setUint32(offset + 4, 0, true); offset += 8; });
		const wasmMinDataAddr = 4096 + 8192;
		if (offset >= wasmMinDataAddr) throw new Error("total length of command line and environment variables exceeds limit");
		this._inst.exports.run(argc, argv);
		if (this.exited) this._resolveExitPromise();
		await this._exitPromise;
	}

	_resume() {
		if (this.exited) throw new Error("Go program has already exited");
		this._inst.exports.resume();
		if (this.exited) this._resolveExitPromise();
	}

	_makeFuncWrapper(id) {
		const go = this;
		return function () {
			const event = { id: id, this: this, args: arguments };
			go._pendingEvent = event;
			go._resume();
			return event.result;
		};
	}
}

const go = new Go();
const memory = () => go._inst.exports.mem.buffer;

// ---- 3. the frame loop: requestAnimationFrame is the browser's own; the canvas follows its box -------------------------------
const canvas = document.getElementById("screen");
new ResizeObserver(() => {
	const w = Math.max(1, Math.round(canvas.clientWidth * devicePixelRatio));
	const h = Math.max(1, Math.round(canvas.clientHeight * devicePixelRatio));
	if (canvas.width !== w || canvas.height !== h) { canvas.width = w; canvas.height = h; pushEvent("resize", 0, w, h); }
}).observe(canvas);

// ---- 4. the input ring (doc/wasm/WEB.md §3): events into linear memory, drained by Go once a frame ---------------------------
const types = { keydown: 1, keyup: 2, pointermove: 3, pointerdown: 4, pointerup: 5, wheel: 6, resize: 7 };
const named = ["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Space", "Enter", "Escape", "ShiftLeft", "ControlLeft", "Backspace", "Tab"];
function keyCode(code) {
	if (/^Key[A-Z]$/.test(code)) return code.charCodeAt(3) - 64;
	if (/^Digit[0-9]$/.test(code)) return code.charCodeAt(5) - 48 + 27;
	const i = named.indexOf(code);
	return i < 0 ? 0 : 37 + i;
}
let ringAt = 0, ringCap = 0;
function pushEvent(type, code, x, y) {
	if (ringCap === 0 || !go._inst) return;
	const v = new DataView(memory());
	const head = v.getUint32(ringAt, true), tail = v.getUint32(ringAt + 4, true);
	if (((head - tail) >>> 0) >= ringCap) return;
	const at = ringAt + 8 + (head % ringCap) * 16;
	v.setUint32(at, types[type], true);
	v.setUint32(at + 4, code, true);
	v.setFloat32(at + 8, x, true);
	v.setFloat32(at + 12, y, true);
	v.setUint32(ringAt, (head + 1) >>> 0, true);
}
for (const t of ["keydown", "keyup"]) canvas.addEventListener(t, (e) => { pushEvent(t, keyCode(e.code), 0, 0); e.preventDefault(); });
for (const t of ["pointermove", "pointerdown", "pointerup"]) {
	canvas.addEventListener(t, (e) => { const r = canvas.getBoundingClientRect(); pushEvent(t, e.button, (e.clientX - r.left) * devicePixelRatio, (e.clientY - r.top) * devicePixelRatio); });
}
canvas.addEventListener("wheel", (e) => { pushEvent("wheel", 0, e.deltaX, e.deltaY); e.preventDefault(); }, { passive: false });

// ---- 5. the WebGPU bridge (doc/wasm/WEB.md §2): the handle table and the command buffer -------------------------------------
const handles = [null];
let encoderObj = null, pass = null;
const h = (i) => { if (i === 0 || i >= handles.length) throw new RangeError("arbace.gpu: no handle " + i); return handles[i]; };
// the operations' argument counts, by number (wai's GPUOps: the same table)
const nargs = [0, 1, 7, 1, 2, 4, 4, 4, 5, 0, 0, 3, 5, 1];
function exec(p, n) {
	const v = new DataView(memory(), p, n);
	const u = (i) => v.getUint32(4 * i, true), f = (i) => v.getFloat32(4 * i, true);
	for (let i = 0; 4 * i < n;) {
		const op = u(i), a = i + 1;
		if (!(op > 0 && op < nargs.length)) throw new RangeError("arbace.gpu: unknown op " + op);
		switch (op) {
			case 1: encoderObj = h(u(a)).createCommandEncoder(); break;
			case 2: {
				const view = h(u(a)).getCurrentTexture().createView();
				const desc = { colorAttachments: [{ view, loadOp: u(a + 2) === 1 ? "clear" : "load", storeOp: "store",
					clearValue: { r: f(a + 3), g: f(a + 4), b: f(a + 5), a: f(a + 6) } }] };
				if (u(a + 1) !== 0) desc.depthStencilAttachment = { view: h(u(a + 1)).createView(), depthClearValue: 1, depthLoadOp: "clear", depthStoreOp: "store" };
				pass = encoderObj.beginRenderPass(desc);
				break;
			}
			case 3: pass.setPipeline(h(u(a))); break;
			case 4: pass.setBindGroup(u(a), h(u(a + 1))); break;
			case 5: if (u(a + 3) !== 0) pass.setVertexBuffer(u(a), h(u(a + 1)), u(a + 2), u(a + 3)); else pass.setVertexBuffer(u(a), h(u(a + 1)), u(a + 2)); break;
			case 6: {
				const fm = u(a + 1) === 2 ? "uint32" : "uint16";
				if (u(a + 3) !== 0) pass.setIndexBuffer(h(u(a)), fm, u(a + 2), u(a + 3)); else pass.setIndexBuffer(h(u(a)), fm, u(a + 2));
				break;
			}
			case 7: pass.draw(u(a), u(a + 1), u(a + 2), u(a + 3)); break;
			case 8: pass.drawIndexed(u(a), u(a + 1), u(a + 2), v.getInt32(4 * (a + 3), true), u(a + 4)); break;
			case 9: pass.end(); pass = null; break;
			case 10: pass = encoderObj.beginComputePass(); break;
			case 11: pass.dispatchWorkgroups(u(a), u(a + 1), u(a + 2)); break;
			case 12: h(u(a)).queue.writeBuffer(h(u(a + 1)), u(a + 2), new Uint8Array(memory(), u(a + 3), u(a + 4))); break;
			case 13: h(u(a)).queue.submit([encoderObj.finish()]); encoderObj = null; break;
		}
		i = a + nargs[op];
	}
}
globalThis.arbace = {
	gpu: {
		handle(o) { handles.push(o); return handles.length - 1; },
		release(i) { if (i > 0 && i < handles.length) handles[i] = null; },
		exec,
	},
	input: { ring(p, cap) { ringAt = p; ringCap = cap; } },
};

// ---- 6. the start ------------------------------------------------------------------------------------------------------------
(async () => {
	if (!navigator.gpu) print("arbace: this browser has no WebGPU (the console and the REPL still work)\n");
	try {
		const params = new URLSearchParams(location.search);
		const url = params.get("wasm") || "arbace.wasm";
		go.argv = [url].concat(params.getAll("arg"));
		const { instance } = await WebAssembly.instantiateStreaming(fetch(url), go.importObject);
		await go.run(instance);
		print("arbace: exited\n");
	} catch (err) {
		print("arbace: " + err + "\n");
		console.error(err);
	}
})();
