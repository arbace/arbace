// repr/rust's case of edition 2015's syntax (doc/rust/SPEC.md §12.2, RR4): what later editions
// reject or read otherwise, round-tripped by bin/repr-rust test at --edition 2015.

// anonymous parameters in a trait's methods
trait Shape {
    fn scale(&self, f64) -> Self;
    fn blend(&self, &Self, u8);
    fn named(&self, factor: f64);
}

// bare trait objects (no dyn)
fn area(s: &Shape) -> Box<Fn(u32) -> u32 + Send> {
    let _ = s;
    Box::new(|x| x)
}

// dyn, async, await, try and gen are names here
fn dyn() -> u8 { 1 }
mod async {
    pub fn await() {}
    pub fn r#try() {}
}
struct gen {
    dyn: u8,
}

fn keywords() -> u8 {
    let async = 2;
    let try = 3;
    let g = gen { dyn: async + try };
    async::await();
    g.dyn + dyn()
}

// ... range patterns, deprecated but parsed
fn digits(c: u8) -> bool {
    match c {
        b'0'...b'9' => true,
        _ => false,
    }
}

// extern crate with its 2015 paths
extern crate core;
use core::mem;

fn size() -> usize {
    ::core::mem::size_of::<u8>() + mem::align_of::<u16>()
}
