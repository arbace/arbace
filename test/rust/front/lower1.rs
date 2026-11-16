// RF6's sample (doc/rust/FRONTEND-SURVEY.md, "As built: RF6"): the desugarings and the HIR printer's corners: while let,
// let chains, let-else, labels, ranges of every kind, `?` on Option and Result, struct update, or-patterns, slice
// patterns with bindings, consts and statics, a trait with an associated type and const, where clauses, impl Trait
// arguments, dyn, raw pointers, unsafe blocks, closures with `move` and return types, comments in odd places.
#![allow(dead_code, unused)]

use std::collections::{BTreeMap, HashMap as Map};
use std::fmt::{self, Display};

/* A block comment
   over two lines */
const LIMIT: usize = 0x10;
static mut COUNTER: u32 = 0;
static NAMES: [&str; 2] = ["a", "b"];

#[derive(Debug, Clone, Default)]
struct Config { name: String, level: u8, verbose: bool }

trait Shape {
    type Unit;
    const SIDES: u32;
    fn area(&self) -> f64;
    fn describe(&self) -> String where Self: Sized { format!("{} sides", Self::SIDES) }
}

struct Square(f64);
impl Shape for Square {
    type Unit = f64;
    const SIDES: u32 = 4;
    fn area(&self) -> f64 { self.0 * self.0 }
}

enum Tok<'a> { Num(i64), Word(&'a str), Pair { l: u8, r: u8 }, End }

fn parse(s: &str) -> Result<i64, std::num::ParseIntError> {
    let n: i64 = s.trim().parse()?; // trailing comment
    Ok(n * 2)
}

fn first_even(v: &[i32]) -> Option<i32> {
    let x = *v.iter().find(|x| **x % 2 == 0)?;
    Some(x)
}

fn classify(t: &Tok<'_>) -> &'static str {
    match t {
        Tok::Num(0) | Tok::Num(1) => "small",
        Tok::Num(n) if *n < 0 => "negative",
        Tok::Num(_) => "number",
        Tok::Word(w) if w.len() > 3 => "long",
        Tok::Word(..) => "word",
        Tok::Pair { l, r: 0 } => "left",
        Tok::Pair { .. } => "pair",
        Tok::End => "end",
    }
}

fn slices(v: &[u8]) -> u32 {
    match v {
        [] => 0,
        [one] => *one as u32,
        [first, .., last] if first == last => 2,
        [first, rest @ ..] => *first as u32 + rest.len() as u32,
    }
}

fn total<T>(xs: &[T]) -> T where T: Copy + std::ops::Add<Output = T> + Default {
    let mut acc = T::default();
    for &x in xs { acc = acc + x; }
    acc
}

fn show(x: impl Display, y: &dyn fmt::Debug) -> String { format!("{x} {y:?}") }

fn loops(n: u32) -> u32 {
    let mut i = 0;
    let mut stack = vec![1, 2, 3];
    while let Some(top) = stack.pop() { i += top; }
    'outer: for a in 0..n {
        for b in (0..=a).rev() {
            if a * b > 10 { break 'outer; }
            if b == 1 { continue 'outer; }
            i += a + b;
        }
    }
    let r = ..3;
    let s = 2..;
    let t = ..=4;
    let _all = &stack[..];
    i + r.end + s.start + t.end
}

fn chains(v: Option<i32>, w: Option<i32>) -> i32 {
    if let Some(a) = v && let Some(b) = w && a < b { a + b } else if let Some(a) = v { a } else { 0 }
}

fn let_else(s: &str) -> u32 {
    let Some(c) = s.chars().next() else { return 0 };
    c as u32
}

fn raw(p: *const u8, q: *mut u8) -> u8 {
    unsafe {
        COUNTER += 1;
        *q = *p;
        *p.add(0)
    }
}

fn closures() -> i32 {
    let k = 3;
    let add = move |a: i32, b: i32| -> i32 { a + b + k };
    let sq = |x| x * x;
    let c = Config { name: String::from("x"), ..Default::default() };
    add(1, 2) + sq(c.level as i32)
}

fn main() {
    let mut m: Map<&str, u32> = Map::new();
    m.insert("a", 1);
    let b: BTreeMap<u8, u8> = BTreeMap::new();
    println!("{} {:?} {}", parse(" 21 ").unwrap(), first_even(&[1, 4, 6]), classify(&Tok::Word("abcd")));
    println!("{}", slices(&[1, 2, 3]) + total(&[1u32, 2, 3]) + loops(5) + chains(Some(1), Some(2)) as u32);
    println!("{} {} {}", show(1, &"x"), let_else("A"), closures());
    let x = 5u8;
    let y = [0u8; 4];
    println!("{}", raw(&x, &mut y.clone()[0]) + Square(2.0).area() as u8 + LIMIT as u8 + NAMES.len() as u8 + b.len() as u8);
}
