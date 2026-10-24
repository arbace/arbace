# Agenda

The current state of the work. This file is rewritten as things change. For the history, see
[JOURNAL.md](JOURNAL.md) (since 2026-10-20), the journals at the tags
[`arbace-interprets-go1.27.1-v1`](https://github.com/arbace/arbace/blob/arbace-interprets-go1.27.1-v1/doc/JOURNAL.md)
(2026-10-15 to 2026-10-20) and
[`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
(2026-10-11 to 2026-10-15), and the frozen branches' (before).

## State

- The bootstrap starts from the Go-source seed `seed/arbace-seed-go.tar.xz` (`54f77c90…`, 2026-10-24), the
  program printed by the Go-hosted fixpoint, its toolchain (TamaGo's go1.27.1) pinned by
  `seed/goroot.sha256`. `bin/seed --check`, `--build` (stage 0), `--make`, `--verify`.
- Stage 0 hosts every Arbace tool (j2g, repr/go, build/go, c2a, the suite's runner), compiled into it;
  `bin/arbace-go --build` is a Go-hosted build, `--fixpoint` the chain G1 → G2 → G3.
- The program's namespaces are compiled to Go (`ARBACE_AOT=1`, the default since S6): the
  executable 143.7 MB (amd64, with goi's table and digests), a one-run build about 2.5 to 3 minutes.
- The box: the same program as a TamaGo guest of go-whim's monitor (/dev/kvm on amd64; KVM/arm64
  and Hypervisor.framework on arm64), and the box rebuilding Arbace inside itself byte for byte
  (`bin/box-rebuild`, run by hand at milestones).
- The JVM runs only the frozen jar of `arbace-for-java-26-v4` (pinned by hash): the oracle's
  reference, the edges' recordings, `bin/repr-java check --suite`'s back end, `jrt-convert`'s edge
  and the data generators; repr/java and javac run on javac's tool executable (JN6), a JDK's javac
  the reference of javac on Arbace's class files.
- `LICENSE.md` holds all licenses and which files each covers.
- The host: Alpine edge on amd64 with /dev/kvm ([ALPINE.md](ALPINE.md); `bin/alpine-env`).

## Done (brief; the history is in the journals)

- **The JVM work, to the first freeze (2026-10-06 to 2026-10-08).** The class forms, j2a,
  Clojure vendored as `arbace/` and self-hosting, Arbace for Java 26. Frozen on
  `arbace-for-java-26` (tags `-v1` to `-v4`).
- **B1a, Arbace as a native Go executable (2026-10-08 to 2026-10-11).** g2a, a2g, jrt, the
  evaluator, `bin/arbace-go`. Frozen on `arbace-for-golang` (tag `arbace-for-go1.27.1-v1`).
- **After the second freeze (2026-10-11).** `.ae` sources, c2a, the tools renamed, Arbace's own
  version.
- **B1b, the box (2026-10-11 to 2026-10-12).** TamaGo's build as a guest of go-whim's monitor
  ([go/BOX-NOTES.md](go/BOX-NOTES.md)): the boxed REPL, `tamago/` as forms, the store, the
  network, wake on events, TLS with Mozilla's roots. Milestones `arbace-box-amd64-v1` and
  `arbace-box-arm64-v1` (KVM/arm64; Hypervisor.framework on an M2 Max, 42 of 42).
- **The standalone Go-hosted bootstrap, S1-S4 (2026-10-12 to 2026-10-13;
  [STANDALONE-SURVEY.md](STANDALONE-SURVEY.md), [STANDALONE-NOTES.md](STANDALONE-NOTES.md)).**
  The JDK's forms committed, a2g without the JVM's reflection, the tools on the Go executable,
  the seed printed by the fixpoint; the JVM's products removed. Milestone `arbace-seed-is-go-v1`.
- **Arbace inside itself, S5-S8 (2026-10-13 to 2026-10-15).** S5 (the known issues, closed by
  measurement), S7 (tool guests, spawned, the action cache), S6 (the namespaces compiled to Go,
  the default; the size halved, 209.9 → 101.3 MB; the variants merged), S8 (the rebuild in the
  box byte for byte on amd64, in the emulated arm64 VM and on the Mac, 46 of 46). The host VM
  survey's HV1-HV9 decided, HV1 (b) built, HV6/HV7 measured on the Mac. Milestone
  `arbace-inside-itself-v1` (2026-10-15): `bin/gate --full` at `c3fdd36`, `bin/box-rebuild` by
  hand (`3131e754…`).

## Now

- The journal started over after the milestone (the user, 2026-10-15).
- Done (2026-10-15): the docs searched for sloppiness and fixed; the build's speed, first round
  ([STANDALONE-NOTES.md](STANDALONE-NOTES.md), "After the milestone: the build's speed"): a
  one-run build about 2:45 to 3:00 (was about 4:15), a2g alone about 100 s (was 160 to 210), the
  seed remade (`79d4f552…`) and verified. SA160-SA163 decided (a), as built. SA90 (b)'s last
  case: a do evaluated again names the new sibling class in reflection (`6e9af3c`). The box's
  oracle and `bin/box-suite` in `bin/gate --full`.
- Done (2026-10-15): S9, Go's toolchain as forms ([STANDALONE-NOTES.md](STANDALONE-NOTES.md),
  "S9"; STANDALONE-SURVEY.md §7 b1'): `tamago/cmd/`, the 105 packages `compile`, `link` and
  `asm` build from, for linux/amd64, tamago/amd64 and tamago/arm64 (`bin/toolchain-vendor`); the
  nine tools built from the forms byte-identical to the originals'; the box's tool guests built
  from them (SA203 (b)); `verify` and `check` in `bin/gate --full` (SA204 (a)).
- Done (2026-10-15): the build's speed, second round ([STANDALONE-NOTES.md](STANDALONE-NOTES.md),
  "the build's speed, second round", parts A-D): the host's back end run compiled (SA164; about
  half as long), a2g after the compiled namespaces arrive 66 to 75 s (was 123), g2a's collect
  index (SA190; its step about 8 s, was 18 to 21), `jrt` not split (SA180). SA164, SA180, SA181,
  SA190, SA191 decided (a). The seed remade after S9's merge (`9b2a4687…`, was `79d4f552…`) and
  verified: stage 0 = G1 = G2 = G3 (`2ee89825…`).
- Done (2026-10-16): the whole chain in the emulated arm64 VM, nothing given (BX117 (a), at
  `434021a`): the image built in the box = the arm64 host step's, byte for byte (`5e4506de…`),
  in 40 h 29 min (about 247 times the host's 591 s; a2g alone 28.8 h). Seen there: the guest
  waiting on a spawn takes 3.5 vCPUs under emulation, an artifact of emulation (natively a few
  percent of one core; BOX-NOTES.md, BX117; left as it is, the user).
- Done (2026-10-16): the build's speed, third round (STANDALONE-NOTES.md, "third round"): the
  loader in parts (analysis 25 → 15 s), a2g's load parallel (10.7 → 5 s), reachability (19-21 →
  6 s), the compiled tree in several files (read 2 → 0.8 s), ids independent of the checkout's
  path (SA230); the seed remade (`5858e83c…`) and verified.
- Done (2026-10-16): the oracle independent of thread interleavings (two concurrency cases;
  ORACLE.md's rule).
- Done (2026-10-16): Arbace independent of go-whim (its tests in `box/` on the box's own guests,
  the C guests in Go's assembly) and of cgo (dropped with the race detector); GW2-GW9 (a)
  (BOX-NOTES.md).

- Done (2026-10-16): the build's speed, fourth round (STANDALONE-NOTES.md, "fourth round"):
  reachability's scan in mutable state with shared plans (`:reach` 7-10 → 3-4.5 s), the
  analysis' fields indexed and supertypes memoized (16 → 5 s); SA250 (a), SA260 (a); the seed
  remade (`b566d53a…`) and verified.
- Done (2026-10-16): a tools-only seed measured (JOURNAL.md): -14% to -22% of xz, `tzdb.dat`
  stays; not pursued (the user).
- Done (2026-10-17): g2a's printer mode `:full` (Go forms converted with `--full` printed back to
  the original text byte for byte; the text round trip in `bin/gate --full`; G2F1-G2F8).

## Next

- **Done: the milestone `arbace-interprets-go1.27.1-v1`** (the user's goal of 2026-10-19; the name,
  the user, 2026-10-20). Its run is done at main (journal, 2026-10-20, "the milestone run"):
  - goi is complete at the REPL for Go 1.27.1: GR7a-GR7c, GR8a-GR8b, GR9a-GR9d, GR10, the arm64
    box check and GC81, each merged, their proposals (GC35-GC165) built as recommended and recorded
    for the user to revisit.
  - `bin/goi check`: 1,471 of 1,837 pass, 0 fail, 0 error. The other 366 are in 17 recorded
    classes (`bin/goi divergences`; GOI-SPEC, "v1's divergences"): 358 by design, 8 deferred
    (go:embed's 2, GS14 (a); `byte`/`rune`/alias names in messages' 6, GC157). Essential 740 of
    740; the box on amd64 583 of 584, in the emulated arm64 VM 291 of 292 (the half, GC75).
  - `bin/gate --full` (its goi step run again alone after `babde67d`), the fixpoint, the seed
    `fcc37393…` (`fca6d187`) verified (stage 0 `199e126b…`).
  - The Mac (the user, 2026-10-20): `./mac.sh goi`, all 584 programs, 582 pass, 2 known (in
    classes), 46 s.
  - **Tagged** `arbace-interprets-go1.27.1-v1` (2026-10-20, on main). After it, the emulated arm64
    check over all of box.txt: 582 of 584, 2 known, as the Mac's.
- **The two front-end surveys (2026-10-20, the user: "make plans"):** plans to decide, not builds.
  - Done: the Go parser's survey (doc/go/GO-PARSER-SURVEY.md, merged): recommended Go's go/parser
    from forms run by goi, annotations by goi's checker, gc's syntax for errors (GP1-GP8, 23-32
    days, the box reads Go at GP8); GPS1-GPS11 decided as recommended (the user, 2026-10-20). GP1 done
    (2026-10-20: E-tree 12,275 of 12,275; GPS12-GPS15 decided as built). GP2 done (2026-10-20: the
    loader, headers 12,275 of 12,275, the box's plan unchanged; the program changed, re-seed
    batched with JN2's; GPS16-GPS20 decided as built). GP3 done (2026-10-21: raw forms,
    `bin/repr-go raw-check` 12,158 dumps resolved, 0 differ, 117 raw only; the normal conversion
    unchanged). The seed remade after GP2, JN2 and GP3 (`aa4cb10b…`) and verified. GPS21-GPS26 decided
    as built (the user, 2026-10-21). GP4 done (2026-10-21: the checker's
    resolve mode, E-ann 12,158 of 12,158 equal, none known; GPS27-GPS31 decided as built). GP5 done (2026-10-22: the native reader
    `bin/repr-go read`'s default, the helper the oracle, E-forms 12,158 of 12,158 in `:full` and
    `:lines`, GPS25 (b)'s corrections, the vendoring tools and goi on the native reader, every committed
    tree unchanged; GPS32-GPS37 decided as built). GP6 done (2026-10-22: gc's syntax errors
    by goi running `cmd/compile/internal/syntax` from `tamago/cmd`'s forms, `bin/repr-go
    syntax-errors`; invalid programs on their raw forms for goi; `bin/goi check` 1,604 of 1,837 pass,
    was 1,472; GC47 63 of 64; the program changed, re-seed batched with JN7's; GPS38-GPS44 decided as
    built); GP7 done (2026-10-22: the native reader compiled, go/parser's
    four packages, the dumper, gc's syntax driver and a renamed copy of syntax in goi's table; std
    parsed in 0.5 s, the executable +1.06 MB; GPS45-GPS49 decided as built); the seed remade after GP6, JN7,
    JFS48 (b) and GP7 (`d7303cf8…`) and verified; GP8 done (2026-10-23: the box converts `tamago/goroot`
    byte for byte on amd64, 167 s, a part of std in the emulated arm64 VM; GPS50-GPS56 decided as
    built; on the Mac 2026-10-24: the whole tree in the box byte for byte, 112.5 s): the Go parser plan
    GP1-GP8 complete. GPS49 (c) done (2026-10-24: the raw conversion and layout about twice as fast,
    std's read 139 → 72 s, `tamago-vendor verify` 8:43 → 4:01, `toolchain-vendor verify` 31:37 → 13:04,
    every output unchanged; GPS57-GPS59, GPS66, GPS67 decided as built; the re-seed batched). `bin/gate --full` on main: see the Java line.
  - Done: the Java front end's survey (doc/classes/JAVA-FRONTEND-SURVEY.md, merged): recommended
    javac itself as Java forms run by Arbace (JF1-JF6, 13-20 days, the JVM out of repr/java), a
    native front end later; JFS1-JFS11 decided as recommended (the user, 2026-10-20), the
    milestones named JN1-JN9. JN1 done (2026-10-20: javac's
    sources on Arbace parse and attribute as on the JVM; a tool executable of 183 MB; JN2 1-2
    days, JN3 3-4); JFS12-JFS17 decided as recommended (the user). JN2 done (2026-10-20:
    javac's forms in `jdk/javac/`, unpatched, `bin/javac-vendor`; JFS18-JFS22 decided as built);
    JN3 done (2026-10-21: javac on Arbace compiles the baseline, the samples, java.base's closure
    and java.base itself byte for byte as the JVM's javac, `bin/javac-vendor classes`, a `javac`
    step in `bin/gate --full`; the program changed, re-seed batched with GP4's; JFS23-JFS26
    decided as built). JN4 done (2026-10-21: repr/java's read on the tool, every corpus byte for
    byte the JVM reader's, `bin/repr-java host-check`; JFS27-JFS30 decided as built). JN5 done (2026-10-21: javac's forms
    read by javac on Arbace byte for byte, the tool repr/java's default host, its --full steps on
    the tool; JFS31-JFS36 decided as built). JN6 done (2026-10-22: the JVM
    leaves repr/java, no JVM under `bin/repr-java` by an strace trace, `check --jdk` on the tool over
    68 modules, `jrt.jrt-sources` on the tools' host, `:jdk` the platform's pin; JFS37-JFS42 decided
    as built); JN7 done (2026-10-22: the generators read by
    repr/java and run on javac's tool executable, their outputs the JDK build's but CaseFolding's boot
    JDK entries; `jrt.classfile`, the edge on the tools' host; `bin/jrt-convert` starts no JVM;
    JFS43-JFS50 decided as built); JFS48 (b) done (2026-10-22: javac's generated sources
    by compileproperties and propertiesparser on the tool, 20 of 20 the build tree's, no JVM;
    `FlagsEnum` still copied; JFS51-JFS54 decided as built); JFS52 (b) done (2026-10-22: `FlagsEnum` by flagsgenerator on the
    tool through javax.tools' API on javac on Arbace, no generated source copied; JFS55-JFS58 decided,
    JFS58 (a) done: `jdk/edge.edn` re-recorded, stale since GP7); JN8 done (2026-10-23: the native Java 26
    parser, javac's trees with positions for 52,465 files and 8,000 mutants, none known;
    `bin/repr-java parse-check` in `--full`; JFS59-JFS66 decided as built); JFS66 (b) done (2026-10-23: the
    converter on an interface over the tree and attribution by position, the native trees giving the
    same forms but 3 known files; the split: 69% of lists need at most types, 98% overload; JN9
    re-estimated 19-34 days; JFS67-JFS71 decided); JFS70 (b) done (2026-10-23: `resolve.ae`'s pins
    independent of the javac run's history, the known list empty, no committed form changed; `tree.ae`'s
    notice; JFS72 (b) decided); `bin/gate --full`'s failures fixed (2026-10-24): the edge (GP8's `OpenOption`), the round trip's
    stale cached originals (not the reader), `nosplit.go` by a per-program limit; `bin/j2g-edge verify`
    in the essential gate. The seed remade (`54f77c90…`) and verified. JN9a done (2026-10-24: the native
    attribution provider for the scope and declarations tiers, every fact equal to javac on Arbace's
    over every corpus, jdk26u's 5,425,925, 0.2% declined to javac by design; JFS71 and JFS72 (b) within
    it; `bin/repr-java attr-check` in `--full`; the rest re-estimated 7-14 days; JFS74-JFS78 decided:
    JFS75 (b) next, then JN9b). The Java conformance survey done (2026-10-24,
    doc/classes/JAVA-CONFORMANCE-SURVEY.md: jdk26u's jtreg tests the corpus; a sample of 2,121 tests
    passing on the JDK: read and class files 100%, interpreted 60%, compiled by j2g 67%; JC1-JC8, 18-29
    days; JCS1-JCS15 decided as recommended). Running: JC1, `bin/java-check` with its interpreted level
    (`java-check-jc1`); JFS75 (b), the converter's model behind an interface
    (`java-model`); JFS65 (b), j2g's array casts (`j2g-array-casts`);. The seed remade after JFS52 (b) and JFS58 (`3ab3b603…`) and verified.
- **After v1:**
  - GC157 (`byte`, `rune` and aliases as names in the checker's types) and go:embed (GS14 (a));
  - staging shared with Futamura's F7 (G16);
  - speed (F10's inline caches, specialization), which would bring the speed-bound programs back;
- Parked (the user, 2026-10-18): KU3 (processes on arm64). A safety classifier stopped both
  agents before they wrote anything. The worktree `ku3` is kept, clean at `80294e9a`. Its
  findings: TamaGo's arm64 `osInit` reads ID_AA64ISAR0_EL1, ID_AA64ISAR1_EL1 and
  ID_AA64PFR0_EL1, which trap at EL0, so the kernel must answer them (proposal KS15, against
  KS2's letter, to decide when KU3 resumes); a Mac step needs the guest package in
  box-vendor's darwin/arm64 configuration and `image()` not skipping without /dev/kvm.
  KU4-KU6 were to go on, on amd64 alone.
- Parked (2026-10-18): KU4 (the process manager, amd64). The classifier stopped its agent too,
  before it wrote anything (worktree `ku4`, clean at `728440fe`). Its findings are in the journal
  (2026-10-18, "KU4 parked"). The KU series waits for the user's direction.

- Done (2026-10-18): F5 (guarded direct linking, geometric mean 1.10), `repr/clojure print`, `repr/java`'s
  `:full` mode, the evaluator's compile race fixed. F5.2 found (code layout), CP1 built, the seed
  remade (`910b4b8f…`). F6 done (2026-10-18: primitive fns and speculative workers, `fib` 0.98 →
  0.53 ms, geometric mean 1.11×; F6.1-F6.3 decided as built). The seed remade (`ed8a4720…`). F7 done (2026-10-18: `defnode` for five kinds, the evaluator 14%
  faster; F7.1-F7.4 decided as recommended: F1 (a) for expressions, (c) for storage and control,
  F8 (b); `bin/generated-check` in the essential gate). The seed remade (`982e071c…`).

- The direction (the user, 2026-10-17): Go forms first-class at the REPL, interpreted like
  Arbace and Java forms; the three dialects at the same level there "when it is appropriate";
  Haskell forms stay data. The survey written (doc/GO-FORMS-REPL-SURVEY.md): recommended
  Arbace's own interpreter of Go forms (a type checker in Arbace, typed closures, a table of the
  program's compiled packages), milestones GR0-GR6, 22-33 days; proposals G1-G17 open; the build
  after F3, F4 and the renames. G1-G17 decided as recommended (2026-10-17); GR0 done (doc/go/GOI-SPEC.md,
  GS1-GS15 decided as recommended); GR1 and GR2 done (the compiled-packages table,
  GoValue; the checker); GR3 done (2026-10-18: the interpreter's core, 532 of `$GOROOT/test`'s
  run programs pass, none fail; GC15-GC18 decided as built); GR4 done (2026-10-18: methods, interfaces, concurrency; 832 pass;
  GC19-GC22 decided as built); GR5 done (the crossings, the package `arbace`, GoPanic, traces,
  printing, GS7, GS10, `go/run-compiled`, the oracle's `forms/go`; GC23-GC25 as built, GC26 (b));
  GR5.1 done (GC26 (b): lines stored only where a statement can panic or call; GC27 as built); GR6 done (generics: 186 of typeparam's 200 run
  programs, `bin/goi check` 1,036 pass, the annotation check over every declaration; GC28-GC34 as
  built); GR7a done (goi checked in the box, `bin/goi check
  --box` 565 of 566; `forms/go` in the box's oracle; recursive types as shapes; `bin/goi check` 1,049
  pass; GC35-GC42 as built; re-seed with GR7b); GR7b running.

- Done (2026-10-17): the rename (doc/NAMING-SURVEY.md, "The rename as built"): j2a → `repr/java`
  (`bin/repr-java`), g2a → `repr/go` (`bin/repr-go`), its build → `build/go` (`bin/build-go`),
  a2g → `j2g` (the printed `j2g_*` names, `J2G_*`), `G2A_GOROOT` → `ARBACE_GOROOT`, the old
  variables guarded; "Java forms" in prose; c2a and `arbace.classes.*` unchanged; R1-R5 decided.
  The seed remade (`56fabe20…`) and verified; R4 done (`tamago/` converted again, the guests
  identical). Done: repr/go's directory tests (`bin/repr-go roundtrip --dirs [--text]`, 251 to 254
  tests per configuration, every level; D1-D3 decided, in `bin/gate --full`). Done: F2 (`bin/diffcheck`, 12,449 of 12,471 cases agree; F2.1-F2.6 decided).
- `repr/haskell`'s spec written (doc/haskell/SPEC.md; S1-S36; HS1-HS12 decided as recommended).
  R1 done (2026-10-17: the tree and lines levels over GHC 9.14.1's base libraries, 483 of 483
  modules; HR1-HR5); R2 done (the interface level, 483 of 483; HR6-HR8); R3 done (`:full`, the
  text level, 483 of 483 byte for byte; HR9-HR13); R4 and R5 done (the layout
  printer; the other boot libraries: 1,226 modules, HR14-HR20); R6 done (2026-10-18: .hsc, .lhs, boot files; 1,345 modules; HR21-HR24 decided as built); HR25 done (2026-10-19: HR26 (b), HR27 (a), HR28 (a), the user: the module view writes CPP's
  disagreeing markers as `{-# LINE #-}`; 1,331 of 1,345 modules with equal trees of mode `:full`, 14 known). Done: `repr/java print` (the round trip over Clojure's Java, the samples and the JDK;
  JP1-JP4); running: (Java forms → Java,
  with a round trip; branch `repr-java-print`).
- Done (2026-10-17): repr/java print. Formerly planned: after the rename (the user, 2026-10-17: "Create jaf print after the naming debate settled"):
  `repr/java print`, Java's bridge printing back, class forms → Java source, with a round trip like
  repr/go's (Java → forms → Java → forms, the two forms equal; the text level as repr/go's
  `:full`); R1: today's `arbace.repr.java.print` (forms as `.ae` text) becomes `layout` with it.
- The survey of Futamura's projections written (doc/FUTAMURA-SURVEY.md; its experiment on the
  local branch `futamura-survey`); decided as suggested (the user, 2026-10-17): after the rename,
  F2 (per-form differential checks), then F3 and F4 (arithmetic fast paths, no initialization
  guards for the classes initialized at start; one re-seed, preceded by the rename of c2a to
  `repr/clojure` and of `arbace.aot` to `c2j`: the user, 2026-10-17), then F5 on the numbers,
  F6, F7. Measured there: `fib`
  6.40 → 0.168 ms (the JVM 0.16) by a `Long` fast path, var speculation and an unboxed worker;
  `loop-long` 11.87 → 1.09 ms without the class-initialization guards.
- Done (2026-10-17): the box's checks on the Mac under macOS 26.7.1: all passed (the checklist 46
  of 46, the rebuild in the box = Linux's byte for byte in 180 s, the clock about 5 ppm).
- `repr/rust`: the survey written (doc/REPR-RUST-SURVEY.md, 2026-10-17; ../rust-lisp as prior
  art): recommended a bridge only, rust-lisp's grammar respelled, a helper on the host's rustc
  with its `rustc-dev` pinned; RS1-RS17 decided; its spec written (doc/rust/SPEC.md, RSS1-RSS10 decided as
  recommended); RR1 and RR2 done (the tree and lines levels, 1,074 of 1,074; the interface level,
  5 of 5 crates); RR3 done (2026-10-18: `:full`, the text level, 1,074 of 1,074 byte for byte;
  RR-P6-RR-P8 decided as built); RR4 done (2026-10-18: the rest of `library/`, the compiler, a
  pinned crate set, the pretty layout: 13,397 files; RR-P9-RR-P12 decided as built); RR5 done (the UI suite as a fifth corpus, the behaviour level
  `roundtrip --run`, std from printed files `--std`; RR-P13-RR-P17 as built); RR6 done (HIR, MIR and objects from the printed files,
  `roundtrip --code`, opt-in; `include!`'s expression files; RR-P18-RR-P22 as built). The
  milestones of SPEC §12.1 are all built.

- The standalone Arbace: no Java binary compatibility, with Java at the source level through repr/java.
  The language is Clojure plus class forms, and later forms for Go. The goal is a
  self-sustaining REPL in a virtual sandbox, in Arbace down to the bare metal ISA, in the style
  of Go plus TamaGo. Targets: /dev/kvm on amd64 (Alpine edge Linux host) and
  Hypervisor.framework on arm64 (macOS host). Open: the runtime's design, Go-inspired (scheduler,
  GC), with proper tail calls possible.
- Futamura's projections (the user, 2026-10-14): Arbace's compiler is written by hand, not
  derived, but it has the pairs the projections relate: the evaluated build (the evaluator and
  the image) against the compiled one (S6), and the class forms' interpreter
  (`arbace/classes/interp`) against j2g, kept in step by the suite and the oracle, not by
  construction. To survey: fns as Go closures (SA156 (c), closure generation, also a Go type
  fewer per fn); code generation (T4) by partial evaluation of the evaluator, ahead of time, in
  the way of Truffle/Graal (speculating on vars, falling back when one is redefined); the second
  projection, j2g derived from the interpreter, one definition of each language's semantics
  (Clojure, the class forms, later Go). The cost: binding times in a dynamic language (vars,
  `eval`, macros at load time).
- A kernel and a user part in the box: the survey written (doc/go/KERNEL-USER-SURVEY.md,
  2026-10-17): recommended a TamaGo runtime per process at EL0 (the same image, a page table per
  process), language isolation inside processes, S7's guests where virtualization exists, one
  spawn request; SYSCALL round trip 24.5 ns measured at ring 3; plan KU0-KU6, 17-27 days;
  K1-K12 decided as recommended; KU0 done (doc/go/PROCESS-SPEC.md; KS1-KS12 decided as
  recommended); KU1 and KU2 done (amd64 processes at ring 3, a process on the user board equal to
  a guest byte for byte; KS13 (a), KS14 (a)); KU3 and KU4 parked (the classifier). The diffcheck flakes' cause, j2g's
  second round, fixed (2026-10-18).
- The mapped start measured without nested KVM.
- A note from the user (2026-10-13): the tooling of the Mac GUI app Moshi Go may be useful for
  Arbace, to study when Arbace's REPL gets a user interface beyond the terminal. Its build info
  (`go version -m`, on the user's Mac): module `moshi-go` at `1516bf8173a5` (2026-10-12), go1.27.1
  (as ours), darwin/arm64, `CGO_ENABLED=0`, `-trimpath`, tag `mygo_noinspector`. Its deps:
  `github.com/egoist/mygo` v0.4.5 replaced by the fork `github.com/rjyo/mygo` (the GUI toolkit),
  `github.com/ebitengine/purego` v0.11.1 (as the box's launcher uses), `go-text/typesetting`
  v0.3.5, `golang.org/x/image`, `alecthomas/chroma/v2` (highlighting), `yuin/goldmark`
  (Markdown), `creack/pty` and `charmbracelet/x` (pty, conpty, term, termios: a terminal),
  `coder/websocket`, `dlclark/regexp2/v2`. A pure-Go, cgo-free GUI on macOS is the same shape
  as the box's launcher there.
- A touch terminal for Arbace's REPL, a clean-room reimplementation of Moshi's iOS terminal
  interface (the user, 2026-10-14; long term, for discussion): survey and plan in
  [IOS-REPL-SURVEY.md](IOS-REPL-SURVEY.md), proposals IR1-IR13 open; more screenshots from the
  user would answer its §3.
