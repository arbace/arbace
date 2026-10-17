#!/usr/bin/env rust-script
//! repr/rust's round trip of the constructs std's five crates use rarely or not at all
//! (bin/repr-rust test, step 4): it parses with rustc 1.99.1 (edition 2024); it is not meant to
//! compile.
#![allow(unused)]
#![feature(never_type, try_blocks, gen_blocks, deref_patterns, box_patterns)]

use ::core::mem::{self as m, swap};
use std::{collections::*, io::{self, Write as _}};

pub(crate) struct Unit;
pub struct Tuple<T>(pub T, u8) where T: Clone;
struct Named<'a, T: 'a + ?Sized, const N: usize = 3> {
    pub(in crate::outer) r: &'a T,
    a: [u8; N],
}
union U { a: u32, b: f32 }
enum E { A = 1, B(u8, u16), C { x: i32 }, D }
trait Alias = Clone + Send;
pub unsafe auto trait Marker {}
trait Tr<T = u8>: Sized where Self: Copy {
    type Out: Default;
    const K: usize = 4;
    fn f(&self, x: T) -> Self::Out;
    fn g<'a>(self: &'a Self) {}
}
impl<T: Copy> Tr for (T,) {
    type Out = ();
    fn f(&self, _: u8) {}
}
impl !Send for Unit {}
extern "C" {
    fn ext(x: i32, ...) -> i32;
    static mut COUNT: u32;
}
macro_rules! m2 ( ($x:expr) => { $x + 1 } );
static S: &str = r#"a "raw" string"#;
const C: (u8, char, &[u8], f64) = (b'\x7f', '\u{1F600}', b"by\xffte", -1.5e3);
type Fp = for<'a> unsafe extern "C" fn(&'a u8, ...) -> !;

fn exprs(x: i32, v: &mut Vec<i32>) -> Option<i32> {
    let y = -x - -1 * (x + 2) / 3 % 4 << 1 >> 2 & 3 | 4 ^ 5;
    let z = !(x == 1 || x != 2 && x < 3) as u8 as i32;
    let Some(w) = v.first() else { return None };
    'outer: loop {
        while let Some(q) = v.pop() {
            if q > 3 { continue 'outer } else if q < 0 { break 'outer; }
        }
        break;
    }
    let lb = 'blk: { if x > 0 { break 'blk 1 } 2 };
    let c = move |a: i32, (b, _): (i32, u8)| -> i32 { a + b };
    let d = async move { 1 };
    let r = match (x, y) {
        (0, _) | (_, 0) => 1,
        (n @ 1..=9, m) if m > 0 => n,
        (i32::MIN..0, _) => -1,
        _ => { 0 }
    };
    let t: Result<i32, ()> = try { 1? };
    let arr = [0u8; 16];
    let s = Named::<'_, str> { r: "x", a: [1; 3] };
    let Named { r, a: [first, .., last], .. } = s;
    let tup = (1,);
    let rng = ..=5;
    v[0] += 1;
    *v.get_mut(1)? = (x as i64 + 1) as i32;
    unsafe { m::swap(&mut 1, &mut 2) };
    vec![1, 2, 3].iter().map(|e| e * 2).filter(|&e| e > 2).count();
    println!("{} {x}", #[allow(unused)] 1);
    Some(x.max(1).min(9))
}

fn pats(b: Box<i32>, o: &Option<String>) {
    let box n = b;
    if let Some(ref s) | Some(ref s) = *o {}
    match o { &Some(ref mut s) => {} &None => {} }
}
