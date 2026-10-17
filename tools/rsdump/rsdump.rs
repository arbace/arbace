// repr/rust's helper (doc/rust/SPEC.md §10; doc/rust/RUST-NOTES.md): rustc's parsed tree of one
// Rust file (`rustc_ast::Crate`, before macro expansion, its out-of-line modules unloaded) as
// Clojure-reader forms, through rustc's own `Debug` impls, transcoded generically. It decides
// nothing about forms: Arbace's converter (arbace.repr.rust.convert) does. Written anew for
// Arbace (SPEC RSS8), from the survey's experiment (doc/REPR-RUST-SURVEY.md, Appendix A, E3);
// rustc's `-Zunpretty=ast-tree` and rust-lisp's `check.rs` its models. A `rustc_private` driver on
// the host's rustc, linked dynamically to its `librustc_driver`; built by `bin/repr-rust helper`.
//
//   rsdump dump  -o DIR --edition E [--helper-hash H] FILE...   DIR/FILE.edn per file
//   rsdump tree  -o DIR --edition E FILE...                      DIR/FILE.tree, DIR/FILE.lines
//   rsdump lines --edition E FILE                                the lines on standard output
//
// FILE is the path rustc is given (relative to the directory it runs in): the file name of every
// span. A dump is `(rsdump/config {...})`, the tree, then the facts the tree does not give
// directly (SPEC §10.3): `(rsdump/mac-exprs [[OPEN :exprs [Expr...]] [OPEN :repeat [Expr Expr]]
// ...])`, each macro call's tokens parsed by rustc's parser as `expr (, expr)*` (or `e; n` in
// brackets) when they parse, keyed by the span of the call's open delimiter, and `(rsdump/meta
// [[SPAN MetaItem] ...])`, each attribute's meta item (`Attribute::meta`), keyed by its span. A
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
extern crate rustc_interface;
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
    /// The tokens of a macro call parsed as `expr (, expr)*`, or `e; n` in brackets
    fn parse_args(&self, mac: &MacCall) -> Option<(bool, Vec<Box<Expr>>)> {
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
        if self.psess.dcx().has_errors().is_some() {
            self.psess.dcx().reset_err_count();
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

// ---- the driver

#[derive(Clone, Copy, PartialEq)]
enum Verb {
    Dump,
    Tree,
    Lines,
}

struct Run {
    verb: Verb,
    file: String,
    /// dump: the dump's text; tree: (tree, lines); lines: the lines
    out: Option<Result<Vec<String>, String>>,
}

impl Callbacks for Run {
    fn after_crate_root_parsing(&mut self, compiler: &Compiler, krate: &mut rustc_ast::Crate) -> Compilation {
        if compiler.sess.dcx().has_errors().is_some() {
            self.out = Some(Err("rustc's parser reported errors".into()));
            return Compilation::Stop;
        }
        let text = format!("{:?}", krate);
        let r = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| match self.verb {
            Verb::Dump => {
                let tree = transcode(&text, &self.file, Mode::Dump);
                let psess = ParseSess::with_dcx(DiagCtxt::new(Box::new(SilentEmitter)), compiler.sess.psess.clone_source_map());
                let mut f = Facts { psess: &psess, file: self.file.clone(), mac: Vec::new(), meta: Vec::new() };
                f.visit_crate(krate);
                vec![tree, format!("(rsdump/mac-exprs [{}])", f.mac.join("\n ")), format!("(rsdump/meta [{}])", f.meta.join("\n "))]
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

fn usage() -> ! {
    eprintln!("usage: rsdump dump|tree -o DIR --edition E [--helper-hash H] FILE...\n       rsdump lines --edition E FILE");
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
    let verb = match args.get(1).map(|s| s.as_str()) {
        Some("dump") => Verb::Dump,
        Some("tree") => Verb::Tree,
        Some("lines") => Verb::Lines,
        _ => usage(),
    };
    let mut out_dir: Option<String> = None;
    let mut edition = String::from("2024");
    let mut helper_hash = String::new();
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
        let mut run = Run { verb, file: file.clone(), out: None };
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
                    "(rsdump/config {{:rustc {} :rustc-commit {} :rustc-driver {} :edition {} :file {} :abs {} :sha256 {} :helper {}}})",
                    clj_str(&version), clj_str(&commit), clj_str(&driver), edition, clj_str(file), clj_str(&abs),
                    clj_str(&sha256(&src)), clj_str(&helper_hash)
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
