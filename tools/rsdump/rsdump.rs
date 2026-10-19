// repr/rust's helper (doc/rust/SPEC.md §10; doc/rust/RUST-NOTES.md): rustc's parsed tree of one
// Rust file (`rustc_ast::Crate`, before macro expansion, its out-of-line modules unloaded) as
// Clojure-reader forms, through rustc's own `Debug` impls, transcoded generically. It decides
// nothing about forms: Arbace's converter (arbace.repr.rust.convert) does. Written anew for
// Arbace (SPEC RSS8), from the survey's experiment (doc/REPR-RUST-SURVEY.md, Appendix A, E3);
// rustc's `-Zunpretty=ast-tree` and rust-lisp's `check.rs` its models. A `rustc_private` driver on
// the host's rustc, linked dynamically to its `librustc_driver`; built by `bin/repr-rust helper`.
//
//   rsdump dump  [--full] [--expr] -o DIR --edition E [--helper-hash H] FILE...   DIR/FILE.edn per file
//   rsdump tree  [--expr] -o DIR --edition E FILE...             DIR/FILE.tree, DIR/FILE.lines
//   rsdump lines [--expr] --edition E FILE                       the lines on standard output
//   rsdump rustc [--hir FILE] [--mir FILE] -- RUSTC-ARGS...      rustc, the crate's HIR and MIR
//                                                                written after analysis (RR6)
//
// With --expr each FILE is an expression file of include! (RR6, SPEC RSS6): parsed as one
// expression, as include! in an expression's place parses it (nothing stripped), the dump's tree
// the expression's `Expr` (its config :expr true).
//
// FILE is the path rustc is given (relative to the directory it runs in): the file name of every
// span. A dump is `(rsdump/config {...})`, the tree, then the facts the tree does not give
// directly (SPEC §10.3): `(rsdump/mac-exprs [[OPEN :exprs [Expr...]] [OPEN :repeat [Expr Expr]]
// ...])`, each macro call's tokens parsed by rustc's parser as `expr (, expr)*` (or `e; n` in
// brackets) when they parse, keyed by the span of the call's open delimiter, and `(rsdump/meta
// [[SPAN MetaItem] ...])`, each attribute's meta item (`Attribute::meta`), keyed by its span, and
// `(rsdump/comments [[L C L2 C2] ...])`, the plain comments `rustc_lexer` finds in the file (for
// mode :lines' `;` comments, RR-P1), or with `--full` `(rsdump/tokens [[KIND L C L2 C2] ...])`,
// all its tokens and comments but white space (mode :full, SPEC §9.2, RR3). A
// file rustc's parser rejects, or parses with recovered errors, is `(rsdump/error "...")` and the
// exit status is 1.
//
// The transcoding: `Name { a: x }` -> `(Name :a x)`, `Name(x)` -> `(Name x)`, `[..]` -> vectors,
// an anonymous tuple -> `(tuple ...)`, strings re-escaped for the reader (controls and DEL as
// \uXXXX), identifiers `x#0` -> `(id "x" 0)` (`r#` only before an identifier character: `r#0` is
// the identifier `r`, SPEC S25), lifetimes `'a#0` -> `(id "'a" 0)`, `lifetime(ID: 'a)` ->
// `(lifetime "'a")`, characters -> `(char N)`, spans `FILE:l1:c1: l2:c2 (#n)` -> `[l1 c1 l2 c2]`
// (`[... n]` for a context other than 0), `no-location` -> nil; node ids and the token streams
// rustc collects for items (`tokens: Some(LazyAttrTokenStream(..))`) dropped. `tree` writes the
// same without spans, ids, syntax contexts, token spacing and collected streams (SPEC §11.2,
// N1-N4), one compound per line; `lines` the start line of every span, in the tree's order, with
// the node and field it belongs to (`-` for a dummy span).
#![feature(rustc_private)]
extern crate rustc_ast;
extern crate rustc_driver;
extern crate rustc_errors;
extern crate rustc_hir;
extern crate rustc_hir_pretty;
extern crate rustc_interface;
extern crate rustc_lexer;
extern crate rustc_middle;
extern crate rustc_parse;
extern crate rustc_session;
extern crate rustc_span;

use rustc_ast::token::{Delimiter, TokenKind};
use rustc_ast::visit::{self, Visitor};
use rustc_ast::{AttrKind, Attribute, Expr, MacCall};
use rustc_driver::{Callbacks, Compilation};
use rustc_errors::DiagCtxt;
use rustc_errors::emitter::SilentEmitter;
use rustc_interface::interface::Compiler;
use rustc_session::parse::ParseSess;
use std::fmt::Write;

const PIN: &str = include_str!("RUSTC_VERSION");

fn pin(key: &str) -> &'static str {
    for l in PIN.lines() {
        if let Some(v) = l.strip_prefix(key) {
            if let Some(v) = v.strip_prefix(' ') {
                return v.trim();
            }
        }
    }
    panic!("rsdump: RUSTC_VERSION has no {key}")
}

/// The rustc this helper runs on: (version, commit, driver hash)
fn running() -> (String, String, String) {
    let v = rustc_interface::util::rustc_version_str().unwrap_or("");
    let version = v.split(' ').next().unwrap_or("").to_string();
    let commit = v.split('(').nth(1).and_then(|s| s.split(' ').next()).unwrap_or("").to_string();
    let maps = std::fs::read_to_string("/proc/self/maps").unwrap_or_default();
    let driver = maps
        .lines()
        .filter_map(|l| l.split("librustc_driver-").nth(1))
        .filter_map(|s| s.strip_suffix(".so"))
        .next()
        .unwrap_or("")
        .to_string();
    (version, commit, driver)
}

// ---- SHA-256 (FIPS 180-4): rustc ships no crate the helper may use for it

fn sha256(data: &[u8]) -> String {
    const K: [u32; 64] = [
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
    ];
    let mut h: [u32; 8] = [
        0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
    ];
    let mut m = data.to_vec();
    let bits = (data.len() as u64).wrapping_mul(8);
    m.push(0x80);
    while m.len() % 64 != 56 {
        m.push(0);
    }
    m.extend_from_slice(&bits.to_be_bytes());
    for chunk in m.chunks(64) {
        let mut w = [0u32; 64];
        for i in 0..16 {
            w[i] = u32::from_be_bytes([chunk[4 * i], chunk[4 * i + 1], chunk[4 * i + 2], chunk[4 * i + 3]]);
        }
        for i in 16..64 {
            let s0 = w[i - 15].rotate_right(7) ^ w[i - 15].rotate_right(18) ^ (w[i - 15] >> 3);
            let s1 = w[i - 2].rotate_right(17) ^ w[i - 2].rotate_right(19) ^ (w[i - 2] >> 10);
            w[i] = w[i - 16].wrapping_add(s0).wrapping_add(w[i - 7]).wrapping_add(s1);
        }
        let mut v = h;
        for i in 0..64 {
            let s1 = v[4].rotate_right(6) ^ v[4].rotate_right(11) ^ v[4].rotate_right(25);
            let ch = (v[4] & v[5]) ^ (!v[4] & v[6]);
            let t1 = v[7].wrapping_add(s1).wrapping_add(ch).wrapping_add(K[i]).wrapping_add(w[i]);
            let s0 = v[0].rotate_right(2) ^ v[0].rotate_right(13) ^ v[0].rotate_right(22);
            let maj = (v[0] & v[1]) ^ (v[0] & v[2]) ^ (v[1] & v[2]);
            let t2 = s0.wrapping_add(maj);
            v = [t1.wrapping_add(t2), v[0], v[1], v[2], v[3].wrapping_add(t1), v[4], v[5], v[6]];
        }
        for i in 0..8 {
            h[i] = h[i].wrapping_add(v[i]);
        }
    }
    h.iter().map(|x| format!("{x:08x}")).collect()
}

// ---- the transcoder: rustc's `{:?}` text to Clojure-reader forms

#[derive(Clone, Copy, PartialEq)]
enum Mode {
    Dump,
    Tree,
    Lines,
}

struct T<'a> {
    s: &'a [u8],
    i: usize,
    file: &'a [u8],
    mode: Mode,
    out: String,
    /// lines mode: the enclosing compounds' names and the current field
    names: Vec<String>,
    key: String,
    depth: usize,
}

fn is_ident_start(b: u8) -> bool {
    b.is_ascii_alphabetic() || b == b'_' || b >= 0x80
}

impl<'a> T<'a> {
    fn new(s: &'a str, file: &'a str, mode: Mode) -> T<'a> {
        T { s: s.as_bytes(), i: 0, file: file.as_bytes(), mode, out: String::new(), names: Vec::new(), key: String::new(), depth: 0 }
    }
    fn fail(&self, what: &str) -> ! {
        let a = self.i.saturating_sub(80);
        let b = (self.i + 80).min(self.s.len());
        panic!("rsdump: {} at {}: {}", what, self.i, String::from_utf8_lossy(&self.s[a..b]))
    }
    fn peek(&self) -> u8 {
        if self.i < self.s.len() { self.s[self.i] } else { 0 }
    }
    fn at(&self, k: usize) -> u8 {
        if self.i + k < self.s.len() { self.s[self.i + k] } else { 0 }
    }
    fn ws(&mut self) {
        while self.i < self.s.len() && (self.s[self.i] == b' ' || self.s[self.i] == b'\n') {
            self.i += 1;
        }
    }
    fn eat(&mut self, c: u8) -> bool {
        self.ws();
        if self.peek() == c {
            self.i += 1;
            true
        } else {
            false
        }
    }
    fn starts(&self, p: &[u8]) -> bool {
        self.s[self.i..].starts_with(p)
    }
    fn word(&mut self) -> String {
        let st = self.i;
        while self.i < self.s.len() && (self.s[self.i].is_ascii_alphanumeric() || self.s[self.i] == b'_' || self.s[self.i] >= 0x80) {
            self.i += 1;
        }
        String::from_utf8(self.s[st..self.i].to_vec()).unwrap()
    }
    fn emit(&mut self, s: &str) {
        if self.mode != Mode::Lines {
            self.out.push_str(s);
        }
    }
    /// A compound's opening in tree mode: on a line of its own
    fn open(&mut self, s: &str) {
        if self.mode == Mode::Tree {
            self.out.push('\n');
            for _ in 0..self.depth {
                self.out.push(' ');
            }
        }
        self.emit(s);
        self.depth += 1;
    }
    fn close(&mut self, s: &str) {
        self.emit(s);
        self.depth -= 1;
    }
    /// Skips a value without writing it
    fn skip(&mut self) {
        let mode = self.mode;
        let o = std::mem::take(&mut self.out);
        let n = std::mem::take(&mut self.names);
        let k = std::mem::take(&mut self.key);
        self.mode = Mode::Dump;
        self.value();
        self.mode = mode;
        self.out = o;
        self.names = n;
        self.key = k;
    }
    fn seq(&mut self, close: u8) {
        loop {
            if self.eat(close) {
                return;
            }
            self.ws();
            if self.starts(b"NodeId(") {
                self.skip();
            } else {
                self.emit(" ");
                self.key.clear();
                self.value();
            }
            self.eat(b',');
        }
    }
    fn fields(&mut self) {
        loop {
            if self.eat(b'}') {
                return;
            }
            self.ws();
            if self.starts(b"..") {
                self.i += 2;
                self.emit(" :..");
                continue;
            }
            let k = self.word();
            if !self.eat(b':') {
                self.fail("a field without a colon");
            }
            self.ws();
            let drop = k == "id"
                || self.starts(b"NodeId(")
                || (k == "tokens" && (self.starts(b"None") || self.starts(b"Some(LazyAttrTokenStream")));
            if drop {
                self.skip();
            } else {
                self.emit(&format!(" :{k} "));
                self.key = k;
                self.value();
            }
            self.eat(b',');
        }
    }
    /// A Rust `Debug` string or character literal's characters
    fn string(&mut self, q: u8) -> String {
        self.i += 1;
        let mut v = String::new();
        loop {
            let c = self.peek();
            if c == q {
                self.i += 1;
                break;
            }
            if c == 0 {
                self.fail("an unterminated string");
            }
            if c == b'\\' {
                let e = self.at(1);
                self.i += 2;
                match e {
                    b'n' => v.push('\n'),
                    b'r' => v.push('\r'),
                    b't' => v.push('\t'),
                    b'0' => v.push('\0'),
                    b'\\' => v.push('\\'),
                    b'"' => v.push('"'),
                    b'\'' => v.push('\''),
                    b'u' => {
                        let st = self.i + 1;
                        while self.peek() != b'}' {
                            self.i += 1;
                        }
                        let h = std::str::from_utf8(&self.s[st..self.i]).unwrap();
                        self.i += 1;
                        match char::from_u32(u32::from_str_radix(h, 16).unwrap()) {
                            Some(ch) => v.push(ch),
                            None => self.fail("an escape of no character"),
                        }
                    }
                    _ => self.fail("an unknown escape"),
                }
            } else {
                let rest = std::str::from_utf8(&self.s[self.i..(self.i + 4).min(self.s.len())])
                    .or_else(|e| std::str::from_utf8(&self.s[self.i..self.i + e.valid_up_to()]))
                    .unwrap();
                let ch = rest.chars().next().unwrap();
                v.push(ch);
                self.i += ch.len_utf8();
            }
        }
        v
    }
    fn clj(&mut self, s: &str) {
        let mut o = String::from("\"");
        for ch in s.chars() {
            match ch {
                '"' => o.push_str("\\\""),
                '\\' => o.push_str("\\\\"),
                '\n' => o.push_str("\\n"),
                '\t' => o.push_str("\\t"),
                '\r' => o.push_str("\\r"),
                c if (c as u32) < 0x20 || c as u32 == 0x7f => write!(o, "\\u{:04x}", c as u32).unwrap(),
                c if (c as u32) > 0xffff => {
                    // the reader takes a supplementary character as itself
                    o.push(c)
                }
                c => o.push(c),
            }
        }
        o.push('"');
        self.emit(&o);
    }
    fn num(&mut self) -> String {
        let st = self.i;
        if self.peek() == b'-' {
            self.i += 1;
        }
        while self.i < self.s.len() && (self.s[self.i].is_ascii_digit() || self.s[self.i] == b'.' || self.s[self.i] == b'e') {
            self.i += 1;
        }
        String::from_utf8(self.s[st..self.i].to_vec()).unwrap()
    }
    fn name_here(&self) -> String {
        let n = self.names.last().map(|s| s.as_str()).unwrap_or("-");
        if self.key.is_empty() { n.to_string() } else { format!("{}.{}", n, self.key) }
    }
    /// FILE:l1:c1: l2:c2 (#n)
    fn span(&mut self) {
        self.i += self.file.len() + 1;
        let mut ns = Vec::new();
        for k in 0..4 {
            ns.push(self.num());
            if k == 1 {
                self.i += 2;
            } else if k < 3 {
                self.i += 1;
            }
        }
        let save = self.i;
        self.ws();
        let c = if self.peek() == b'(' && self.at(1) == b'#' {
            self.i += 2;
            let n = self.num();
            self.eat(b')');
            n
        } else {
            self.i = save;
            "0".into()
        };
        match self.mode {
            Mode::Dump => {
                if c == "0" {
                    self.emit(&format!("[{} {} {} {}]", ns[0], ns[1], ns[2], ns[3]));
                } else {
                    self.emit(&format!("[{} {} {} {} {}]", ns[0], ns[1], ns[2], ns[3], c));
                }
            }
            Mode::Tree => self.emit("_"),
            Mode::Lines => {
                let n = self.name_here();
                writeln!(self.out, "{} {}", ns[0], n).unwrap();
            }
        }
    }
    fn value(&mut self) {
        self.ws();
        let c = self.peek();
        if self.starts(self.file) && self.s.get(self.i + self.file.len()) == Some(&b':') {
            return self.span();
        }
        if self.starts(b"no-location") {
            self.i += 11;
            let save = self.i;
            self.ws();
            if self.peek() == b'(' && self.at(1) == b'#' {
                while self.peek() != b')' {
                    self.i += 1;
                }
                self.i += 1;
            } else {
                self.i = save;
            }
            match self.mode {
                Mode::Lines => {
                    let n = self.name_here();
                    writeln!(self.out, "- {}", n).unwrap();
                }
                Mode::Tree => self.emit("_"),
                Mode::Dump => self.emit("nil"),
            }
            return;
        }
        match c {
            b'"' => {
                let s = self.string(b'"');
                self.clj(&s);
            }
            b'\'' => {
                // a character 'c' (or an escape), or a lifetime or label identifier 'a#0
                let is_char = self.at(1) == b'\\' || {
                    let rest = &self.s[self.i + 1..(self.i + 6).min(self.s.len())];
                    let n = match rest.first() { Some(&b) if b < 0x80 => 1, Some(&b) if b >= 0xf0 => 4, Some(&b) if b >= 0xe0 => 3, _ => 2 };
                    rest.get(n) == Some(&b'\'')
                };
                if is_char {
                    let s = self.string(b'\'');
                    let ch = s.chars().next().unwrap();
                    self.emit(&format!("(char {})", ch as u32));
                } else {
                    self.i += 1;
                    let mut w = format!("'{}", self.word());
                    if w == "'r" && self.peek() == b'#' && is_ident_start(self.at(1)) {
                        self.i += 1;
                        w = format!("'r#{}", self.word());
                    }
                    self.emit("(id ");
                    self.clj(&w);
                    if self.peek() == b'#' {
                        self.i += 1;
                        let n = self.num();
                        if self.mode == Mode::Dump {
                            self.emit(&format!(" {}", n));
                        }
                    }
                    self.emit(")");
                }
            }
            b'[' => {
                self.i += 1;
                self.names.push("[".into());
                self.open("[");
                self.seq(b']');
                self.close("]");
                self.names.pop();
            }
            b'(' => {
                self.i += 1;
                self.names.push("tuple".into());
                self.open("(tuple");
                self.seq(b')');
                self.close(")");
                self.names.pop();
            }
            b'{' if self.starts(b"{{root}}#") => {
                self.i += 9;
                let c = self.num();
                if self.mode == Mode::Dump {
                    self.emit(&format!("(id \"{{{{root}}}}\" {})", c));
                } else {
                    self.emit("(id \"{{root}}\")");
                }
            }
            b'{' => {
                self.i += 1;
                self.names.push("map".into());
                self.open("(map");
                self.fields();
                self.close(")");
                self.names.pop();
            }
            b'#' => {
                self.i += 1;
                let n = self.num();
                self.emit(&format!("(ctxt {})", n));
            }
            b'-' | b'0'..=b'9' => {
                let n = self.num();
                if self.peek() == b'#' {
                    self.i += 1;
                    let c = self.num();
                    self.emit("(id ");
                    self.clj(&n);
                    if self.mode == Mode::Dump {
                        self.emit(&format!(" {}", c));
                    }
                    self.emit(")");
                } else {
                    self.emit(&n);
                }
            }
            _ => {
                let mut w = self.word();
                if w.is_empty() {
                    self.fail("an unexpected character");
                }
                if w == "r" && self.peek() == b'#' && is_ident_start(self.at(1)) {
                    self.i += 1;
                    w = format!("r#{}", self.word());
                }
                if self.peek() == b'#' {
                    // an identifier: name#ctxt
                    self.i += 1;
                    let n = self.num();
                    self.emit("(id ");
                    self.clj(&w);
                    if self.mode == Mode::Dump {
                        self.emit(&format!(" {}", n));
                    }
                    self.emit(")");
                    return;
                }
                if w == "lifetime" && self.peek() == b'(' {
                    // lifetime(ID: 'name)
                    while self.peek() != b'\'' {
                        self.i += 1;
                    }
                    let st = self.i;
                    while self.peek() != b')' {
                        self.i += 1;
                    }
                    let name = String::from_utf8(self.s[st..self.i].to_vec()).unwrap();
                    self.i += 1;
                    self.emit("(lifetime ");
                    self.clj(&name);
                    self.emit(")");
                    return;
                }
                let save = self.i;
                self.ws();
                if self.mode == Mode::Tree && (w == "Joint" || w == "Alone" || w == "JointHidden" || w == "DelimSpacing") {
                    // token spacing (N3)
                    if w == "DelimSpacing" && self.peek() == b'{' {
                        self.i = save;
                        self.i += 0;
                        self.ws();
                        self.i += 1;
                        let o = std::mem::take(&mut self.out);
                        let m = self.mode;
                        self.mode = Mode::Dump;
                        self.fields();
                        self.mode = m;
                        self.out = o;
                    } else {
                        self.i = save;
                    }
                    self.emit("_");
                    return;
                }
                match self.peek() {
                    b'(' => {
                        self.i += 1;
                        self.names.push(w.clone());
                        self.open(&format!("({}", w));
                        self.seq(b')');
                        self.close(")");
                        self.names.pop();
                    }
                    b'{' => {
                        self.i += 1;
                        self.names.push(w.clone());
                        self.open(&format!("({}", w));
                        self.fields();
                        self.close(")");
                        self.names.pop();
                    }
                    _ => {
                        self.i = save;
                        self.emit(&w);
                    }
                }
            }
        }
    }
}

fn transcode(text: &str, file: &str, mode: Mode) -> String {
    let mut t = T::new(text, file, mode);
    t.value();
    t.ws();
    if t.i != t.s.len() {
        t.fail("trailing text");
    }
    t.out
}

// ---- the facts (SPEC §10.3)

struct Facts<'p> {
    psess: &'p ParseSess,
    file: String,
    mac: Vec<String>,
    meta: Vec<String>,
}

impl<'p> Facts<'p> {
    /// The tokens of a macro call parsed as `expr (, expr)*`, or `e; n` in brackets; a parse
    /// that reported an error, left a delayed bug (a recovery that expects an error later) or
    /// raised a fatal error is none, and the silent context is left empty (a delayed bug left in
    /// it panics when it drops)
    fn parse_args(&self, mac: &MacCall) -> Option<(bool, Vec<Box<Expr>>)> {
        // a fatal error of the parser (FatalError::raise, a panic) is none too
        let r = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| self.parse_args_in(mac))).unwrap_or(None);
        self.psess.dcx().reset_err_count();
        r
    }

    fn parse_args_in(&self, mac: &MacCall) -> Option<(bool, Vec<Box<Expr>>)> {
        let args = &mac.args;
        if !matches!(args.delim, Delimiter::Parenthesis | Delimiter::Bracket) || args.tokens.is_empty() {
            return None;
        }
        self.psess.dcx().reset_err_count();
        let mut p = rustc_parse::parser::Parser::new(self.psess, args.tokens.clone(), Some("macro arguments"));
        let mut exprs = Vec::new();
        let mut repeat = false;
        loop {
            match p.parse_expr() {
                Ok(e) => exprs.push(e),
                Err(err) => {
                    err.cancel();
                    return None;
                }
            }
            if p.token.kind == TokenKind::Eof {
                break;
            }
            if args.delim == Delimiter::Bracket && exprs.len() == 1 && p.token.kind == TokenKind::Semi {
                p.bump();
                match p.parse_expr() {
                    Ok(e) => exprs.push(e),
                    Err(err) => {
                        err.cancel();
                        return None;
                    }
                }
                if p.token.kind != TokenKind::Eof {
                    return None;
                }
                repeat = true;
                break;
            }
            if p.token.kind != TokenKind::Comma {
                return None;
            }
            p.bump();
            if p.token.kind == TokenKind::Eof {
                // a trailing comma: the forms cannot hold it (SPEC §7.1)
                return None;
            }
        }
        if self.psess.dcx().has_errors_or_delayed_bugs().is_some() {
            return None;
        }
        Some((repeat, exprs))
    }
}

impl<'a, 'p> Visitor<'a> for Facts<'p> {
    fn visit_mac_call(&mut self, mac: &'a MacCall) {
        visit::walk_mac(self, mac);
        if let Some((repeat, exprs)) = self.parse_args(mac) {
            let key = transcode(&format!("{:?}", mac.args.dspan.open), &self.file, Mode::Dump);
            let es = transcode(&format!("{:?}", exprs), &self.file, Mode::Dump);
            self.mac.push(format!("[{} {} {}]", key, if repeat { ":repeat" } else { ":exprs" }, es));
            for e in &exprs {
                <Self as Visitor<'_>>::visit_expr(self, e);
            }
        }
    }
    fn visit_attribute(&mut self, attr: &'a Attribute) {
        if let AttrKind::Normal(_) = &attr.kind {
            if let Some(m) = attr.meta() {
                let key = transcode(&format!("{:?}", attr.span), &self.file, Mode::Dump);
                let mt = transcode(&format!("{:?}", m), &self.file, Mode::Dump);
                self.meta.push(format!("[{} {}]", key, mt));
            }
        }
        visit::walk_attribute(self, attr);
    }
}

// ---- the lexer's tokens and comments (SPEC §9.2, §10.3; RR3)

/// The text rustc lexes (its source map's normalization): a UTF-8 byte order mark removed, `\r\n`
/// made `\n`
fn normalized(raw: &str) -> String {
    let s = raw.strip_prefix('\u{feff}').unwrap_or(raw);
    if s.contains("\r\n") { s.replace("\r\n", "\n") } else { s.to_string() }
}

/// `rustc_lexer`'s tokens of the file, white space left out, each `[KIND LINE COL END-LINE
/// END-COL]` (columns in characters from 1, the end exclusive, as rustc's spans count them): KIND
/// `:c` a plain comment, `:doc` a doc comment, `:id` an identifier (raw or not), `:lt` a
/// lifetime, `:lit` a literal, `:p` a punctuation character, `:shebang` the first `#!` line,
/// `:fm` a front matter, `:x` anything else; with comments_only, the plain comments alone. Before
/// edition 2024 a guarded string's prefix `#"` is a `#` and what follows lexed again, as rustc's
/// own lexer does (rustc_parse::lexer, `GuardedStrPrefix`: RR5)
fn lex(raw: &str, comments_only: bool, ed2024: bool, expr: bool) -> String {
    use rustc_lexer::TokenKind as K;
    let src = normalized(raw);
    let mut out = Vec::new();
    // line and column of byte offset `to`, advanced from (pos, line, col)
    let (mut pos, mut line, mut col) = (0usize, 1usize, 1usize);
    let advance = |to: usize, pos: &mut usize, line: &mut usize, col: &mut usize| {
        for ch in src[*pos..to].chars() {
            if ch == '\n' {
                *line += 1;
                *col = 1;
            } else {
                *col += 1;
            }
        }
        *pos = to;
    };
    let mut start = 0usize;
    // an expression file of include! (RR6): parsed with nothing stripped (StripTokens::Nothing),
    // a #! line and --- fences are tokens
    if let Some(n) = rustc_lexer::strip_shebang(&src).filter(|_| !expr) {
        if !comments_only {
            out.push(format!("[:shebang 1 1 1 {}]", src[..n].chars().count() + 1));
        }
        start = n;
    }
    let mut fm = if expr { rustc_lexer::FrontmatterAllowed::No } else { rustc_lexer::FrontmatterAllowed::Yes };
    loop {
    let mut restart = false;
    for t in rustc_lexer::tokenize(&src[start..], fm) {
        let lo = start;
        let guarded = t.kind == K::GuardedStrPrefix && !ed2024;
        let hi = if guarded { start + 1 } else { start + t.len as usize };
        start = hi;
        let k = match t.kind {
            _ if guarded && comments_only => { restart = true; break }
            _ if guarded => ":p",
            K::Whitespace => continue,
            K::LineComment { doc_style: None } | K::BlockComment { doc_style: None, .. } => ":c",
            _ if comments_only => continue,
            K::LineComment { .. } | K::BlockComment { .. } => ":doc",
            K::Ident | K::RawIdent | K::InvalidIdent => ":id",
            K::Lifetime { .. } | K::RawLifetime => ":lt",
            K::Literal { .. } => ":lit",
            K::Frontmatter { .. } => ":fm",
            K::Semi | K::Comma | K::Dot | K::OpenParen | K::CloseParen | K::OpenBrace | K::CloseBrace
            | K::OpenBracket | K::CloseBracket | K::At | K::Pound | K::Tilde | K::Question | K::Colon
            | K::Dollar | K::Eq | K::Bang | K::Lt | K::Gt | K::Minus | K::And | K::Or | K::Plus | K::Star
            | K::Slash | K::Caret | K::Percent => ":p",
            _ => ":x",
        };
        advance(lo, &mut pos, &mut line, &mut col);
        let (l1, c1) = (line, col);
        advance(hi, &mut pos, &mut line, &mut col);
        if comments_only {
            out.push(format!("[{l1} {c1} {line} {col}]"));
        } else {
            out.push(format!("[{k} {l1} {c1} {line} {col}]"));
        }
        if guarded {
            restart = true;
            break;
        }
    }
    if !restart {
        break;
    }
    fm = rustc_lexer::FrontmatterAllowed::No;
    }
    out.join(" ")
}

// ---- the driver

#[derive(Clone, Copy, PartialEq)]
enum Verb {
    Dump,
    Tree,
    Lines,
}

struct Run {
    verb: Verb,
    full: bool,
    /// the file is an expression, as include! in an expression's place parses it (RR6, SPEC
    /// RSS6): rustc is given an empty crate root, and the file is parsed by rustc's parser as one
    /// expression with nothing stripped, which must end the file
    expr: bool,
    file: String,
    /// dump: the dump's text; tree: (tree, lines); lines: the lines
    out: Option<Result<Vec<String>, String>>,
}

impl Callbacks for Run {
    fn config(&mut self, config: &mut rustc_interface::interface::Config) {
        if self.expr {
            config.input = rustc_session::config::Input::Str {
                name: rustc_span::FileName::Custom("rsdump-empty-root".into()),
                input: String::new(),
            };
        }
    }

    fn after_crate_root_parsing(&mut self, compiler: &Compiler, krate: &mut rustc_ast::Crate) -> Compilation {
        if compiler.sess.dcx().has_errors().is_some() {
            self.out = Some(Err("rustc's parser reported errors".into()));
            return Compilation::Stop;
        }
        let parsed_expr = if self.expr {
            let psess = &compiler.sess.psess;
            let mut p = match rustc_parse::new_parser_from_file(psess, std::path::Path::new(&self.file), rustc_parse::lexer::StripTokens::Nothing, None) {
                Ok(p) => p,
                Err(es) => {
                    for e in es {
                        e.cancel();
                    }
                    self.out = Some(Err("rustc's lexer rejects the file".into()));
                    return Compilation::Stop;
                }
            };
            let e = match p.parse_expr() {
                Ok(e) => e,
                Err(e) => {
                    e.cancel();
                    self.out = Some(Err("rustc's parser does not parse the file as an expression".into()));
                    return Compilation::Stop;
                }
            };
            if p.token.kind != TokenKind::Eof || compiler.sess.dcx().has_errors_or_delayed_bugs().is_some() {
                self.out = Some(Err("the file is not one expression (include!'s incomplete_include)".into()));
                return Compilation::Stop;
            }
            Some(e)
        } else {
            None
        };
        let text = match &parsed_expr {
            Some(e) => format!("{:?}", e),
            None => format!("{:?}", krate),
        };
        let r = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| match self.verb {
            Verb::Dump => {
                let tree = transcode(&text, &self.file, Mode::Dump);
                let psess = ParseSess::with_dcx(DiagCtxt::new(Box::new(SilentEmitter)), compiler.sess.psess.clone_source_map());
                let mut f = Facts { psess: &psess, file: self.file.clone(), mac: Vec::new(), meta: Vec::new() };
                match &parsed_expr {
                    Some(e) => f.visit_expr(e),
                    None => f.visit_crate(krate),
                }
                let raw = std::fs::read_to_string(&self.file).unwrap_or_default();
                let lexed = if self.full {
                    format!("(rsdump/tokens [{}])", lex(&raw, false, compiler.sess.edition().at_least_rust_2024(), self.expr))
                } else {
                    format!("(rsdump/comments [{}])", lex(&raw, true, compiler.sess.edition().at_least_rust_2024(), self.expr))
                };
                vec![tree, format!("(rsdump/mac-exprs [{}])", f.mac.join("\n ")), format!("(rsdump/meta [{}])", f.meta.join("\n ")), lexed]
            }
            Verb::Tree => vec![transcode(&text, &self.file, Mode::Tree), transcode(&text, &self.file, Mode::Lines)],
            Verb::Lines => vec![transcode(&text, &self.file, Mode::Lines)],
        }));
        self.out = Some(match r {
            Ok(v) => Ok(v),
            Err(e) => Err(e
                .downcast_ref::<String>()
                .cloned()
                .or_else(|| e.downcast_ref::<&str>().map(|s| s.to_string()))
                .unwrap_or_else(|| "rsdump: the transcoder failed".into())),
        });
        Compilation::Stop
    }
}

fn clj_str(s: &str) -> String {
    let mut o = String::from("\"");
    for ch in s.chars() {
        match ch {
            '"' => o.push_str("\\\""),
            '\\' => o.push_str("\\\\"),
            '\n' => o.push_str("\\n"),
            c if (c as u32) < 0x20 || c as u32 == 0x7f => write!(o, "\\u{:04x}", c as u32).unwrap(),
            c => o.push(c),
        }
    }
    o.push('"');
    o
}

fn write_file(path: &str, text: &str) {
    if let Some(d) = std::path::Path::new(path).parent() {
        std::fs::create_dir_all(d).unwrap();
    }
    std::fs::write(path, text).unwrap_or_else(|e| panic!("rsdump: cannot write {path}: {e}"));
}

// ---- the lowered and code levels (SPEC §11.1, 3 and 4; RR6): rustc run as a driver, writing the
// crate's HIR and MIR after analysis, then going on to code generation as rustc would

/// `rsdump rustc [--hir FILE] [--mir FILE] -- ARGS...`: rustc with ARGS (its own outputs, an rlib,
/// objects, as ARGS say), and after analysis the crate's HIR as `-Zunpretty=hir` prints it but
/// without the root file's comments (`rustc_hir_pretty::print_crate` given no source text: the
/// comments it interleaves are the source's, which mode :lines does not print), and its MIR as
/// `-Zunpretty=mir` prints it (`write_mir_pretty`), but a `#[rustc_comptime]` function's body
/// taken from `mir_for_ctfe`, which 1.99.1's `write_mir_pretty` asks of `optimized_mir` and
/// panics (core's `intrinsics::needs_drop`); paths printed in full (rustc's own callbacks trim
/// them, which its diagnostics' check refuses when no diagnostic follows)
struct Compile {
    hir: Option<String>,
    mir: Option<String>,
}

fn write_mir<'tcx>(tcx: rustc_middle::ty::TyCtxt<'tcx>, w: &mut dyn std::io::Write) -> std::io::Result<()> {
    use rustc_hir::ConstContext;
    use rustc_hir::def::DefKind;
    use rustc_middle::mir::Const;
    use rustc_middle::mir::pretty::MirWriter;
    use rustc_middle::ty::InstanceKind;
    let writer = MirWriter::new(tcx);
    writeln!(w, "// WARNING: This output format is intended for human consumers only")?;
    writeln!(w, "// and is subject to change without notice. Knock yourself out.")?;
    writeln!(w, "// HINT: See also -Z dump-mir for MIR at specific points during compilation.")?;
    let mut first = true;
    for &def_id in tcx.mir_keys(()) {
        if first {
            first = false;
        } else {
            writeln!(w)?;
        }
        let render_body = |w: &mut dyn std::io::Write, body| -> std::io::Result<()> {
            writer.write_mir_fn(body, w)?;
            for body in tcx.promoted_mir(def_id) {
                writeln!(w)?;
                writer.write_mir_fn(body, w)?;
            }
            Ok(())
        };
        if matches!(tcx.def_kind(def_id), DefKind::Fn | DefKind::AssocFn)
            && matches!(tcx.hir_body_const_context(def_id), Some(ConstContext::Const { .. }))
        {
            // a `#[rustc_comptime]` function: only its MIR for CTFE
            writeln!(w, "// MIR FOR CTFE")?;
            render_body(w, tcx.mir_for_ctfe(def_id))?;
        } else if tcx.is_const_fn(def_id) {
            render_body(w, tcx.optimized_mir(def_id))?;
            writeln!(w)?;
            writeln!(w, "// MIR FOR CTFE")?;
            writer.write_mir_fn(tcx.mir_for_ctfe(def_id), w)?;
        } else if let Some((val, ty)) = tcx.trivial_const(def_id) {
            let name = {
                let _g = rustc_middle::ty::print::ForcedImplGuard::new();
                tcx.def_path_str(def_id)
            };
            writeln!(w, "const {}: {} = const {};", name, ty, Const::Val(val, ty))?;
        } else {
            render_body(w, tcx.instance_mir(InstanceKind::Item(def_id.to_def_id())))?;
        }
    }
    Ok(())
}

impl Callbacks for Compile {
    fn after_analysis<'tcx>(&mut self, _compiler: &Compiler, tcx: rustc_middle::ty::TyCtxt<'tcx>) -> Compilation {
        if let Some(p) = &self.hir {
            let sm = tcx.sess.source_map();
            let attrs = |id| tcx.hir_attrs(id);
            let name = tcx.sess.io.input.file_name(tcx.sess);
            let ann = &tcx as &dyn rustc_hir::intravisit::HirTyCtxt<'_>;
            let text = rustc_hir_pretty::print_crate(sm, tcx.hir_root_module(), name, String::new(), &attrs, &ann as &dyn rustc_hir_pretty::PpAnn);
            write_file(p, &text);
        }
        if let Some(p) = &self.mir {
            let mut out = Vec::new();
            write_mir(tcx, &mut out).unwrap_or_else(|e| panic!("rsdump: MIR of {p}: {e}"));
            write_file(p, &String::from_utf8_lossy(&out));
        }
        Compilation::Continue
    }
}

fn compile(args: &[String]) -> ! {
    let mut c = Compile { hir: None, mir: None };
    let mut i = 2;
    while i < args.len() {
        match args[i].as_str() {
            "--hir" => {
                c.hir = args.get(i + 1).cloned();
                i += 2
            }
            "--mir" => {
                c.mir = args.get(i + 1).cloned();
                i += 2
            }
            "--" => {
                i += 1;
                break;
            }
            _ => usage(),
        }
    }
    let mut rargs: Vec<String> = vec!["rustc".into()];
    rargs.extend_from_slice(&args[i..]);
    let code = rustc_driver::catch_with_exit_code(|| rustc_driver::run_compiler(&rargs, &mut c));
    std::process::exit(if code == std::process::ExitCode::SUCCESS { 0 } else { 1 })
}

fn usage() -> ! {
    eprintln!("usage: rsdump dump [--full] [--expr] -o DIR --edition E [--helper-hash H] FILE...\n       rsdump tree [--expr] -o DIR --edition E FILE...\n       rsdump lines [--expr] --edition E FILE\n       rsdump rustc [--hir FILE] [--mir FILE] -- RUSTC-ARGS...");
    std::process::exit(2)
}

fn main() {
    let (version, commit, driver) = running();
    if version != pin("version") || commit != pin("commit") || driver != pin("driver") {
        eprintln!(
            "rsdump: built for rustc {} ({}, librustc_driver-{}), running on {} ({}, librustc_driver-{}): tools/rsdump/RUSTC_VERSION pins the former (SPEC §10.5)",
            pin("version"), pin("commit"), pin("driver"), version, commit, driver
        );
        std::process::exit(2);
    }
    let args: Vec<String> = std::env::args().collect();
    if args.get(1).map(|s| s.as_str()) == Some("rustc") {
        compile(&args);
    }
    let verb = match args.get(1).map(|s| s.as_str()) {
        Some("dump") => Verb::Dump,
        Some("tree") => Verb::Tree,
        Some("lines") => Verb::Lines,
        _ => usage(),
    };
    let mut out_dir: Option<String> = None;
    let mut edition = String::from("2024");
    let mut helper_hash = String::new();
    let mut full = false;
    let mut expr = false;
    let mut files = Vec::new();
    let mut i = 2;
    while i < args.len() {
        match args[i].as_str() {
            "-o" => {
                out_dir = args.get(i + 1).cloned();
                i += 2
            }
            "--edition" => {
                edition = args.get(i + 1).cloned().unwrap_or_else(|| usage());
                i += 2
            }
            "--full" => {
                full = true;
                i += 1
            }
            "--expr" => {
                expr = true;
                i += 1
            }
            "--helper-hash" => {
                helper_hash = args.get(i + 1).cloned().unwrap_or_else(|| usage());
                i += 2
            }
            f => {
                files.push(f.to_string());
                i += 1
            }
        }
    }
    if files.is_empty() || (verb != Verb::Lines && out_dir.is_none()) {
        usage();
    }
    let mut status = 0;
    for file in &files {
        let mut run = Run { verb, full, expr, file: file.clone(), out: None };
        let rargs: Vec<String> = vec!["rsdump".into(), "--edition".into(), edition.clone(), file.clone()];
        let caught = rustc_span::fatal_error::catch_fatal_errors(|| rustc_driver::run_compiler(&rargs, &mut run));
        let res = match (caught, run.out.take()) {
            (_, Some(r)) => r,
            (_, None) => Err("rustc's parser failed".into()),
        };
        let res = res.map_err(|e| {
            status = 1;
            eprintln!("rsdump: {file}: {e}");
            e
        });
        match verb {
            Verb::Dump => {
                let src = std::fs::read(file).unwrap_or_default();
                let abs = std::fs::canonicalize(file).map(|p| p.display().to_string()).unwrap_or_default();
                let config = format!(
                    "(rsdump/config {{:rustc {} :rustc-commit {} :rustc-driver {} :edition {} :file {} :abs {} :sha256 {} :helper {}{}}})",
                    clj_str(&version), clj_str(&commit), clj_str(&driver), edition, clj_str(file), clj_str(&abs),
                    clj_str(&sha256(&src)), clj_str(&helper_hash), if expr { " :expr true" } else { "" }
                );
                let body = match &res {
                    Ok(v) => v.join("\n"),
                    Err(e) => format!("(rsdump/error {})", clj_str(e)),
                };
                write_file(&format!("{}/{}.edn", out_dir.as_ref().unwrap(), file), &format!("{config}\n{body}\n"));
            }
            Verb::Tree => {
                let d = out_dir.as_ref().unwrap();
                match &res {
                    Ok(v) => {
                        write_file(&format!("{d}/{file}.tree"), &format!("{}\n", v[0].trim_start()));
                        write_file(&format!("{d}/{file}.lines"), &v[1]);
                    }
                    Err(e) => {
                        write_file(&format!("{d}/{file}.tree"), &format!("rsdump/error {e}\n"));
                        write_file(&format!("{d}/{file}.lines"), "");
                    }
                }
            }
            Verb::Lines => {
                if let Ok(v) = &res {
                    print!("{}", v[0]);
                }
            }
        }
    }
    std::process::exit(status);
}
