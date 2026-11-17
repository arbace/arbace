// rsi's sample of fixed-width vectors (RES6 (a); doc/rust/EVAL.md §5): hashbrown's SSE2 group (HashMap, HashSet), core::simd's
// portable vectors (splat, lanewise arithmetic and compares, masks to bitmasks, shuffles, reductions), each result printed.
#![feature(portable_simd)]
use std::collections::{HashMap, HashSet};
use std::hint::black_box as bb;
use std::simd::prelude::*;
use std::simd::{simd_swizzle};

fn main() {
    let mut m: HashMap<String, usize> = HashMap::new();
    for (i, w) in "the quick brown fox jumps over the lazy dog the end".split(' ').enumerate() { *m.entry(w.to_string()).or_insert(0) += i; }
    let mut v: Vec<_> = m.iter().collect(); v.sort(); println!("{:?}", v);
    let s: HashSet<u32> = (0..200).map(|x| x * 7 % 61).collect(); let mut sv: Vec<_> = s.into_iter().collect(); sv.sort(); println!("{} {:?}", sv.len(), &sv[..10]);
    m.retain(|k, _| k.len() > 3); let mut ks: Vec<_> = m.keys().cloned().collect(); ks.sort(); println!("{:?}", ks);

    let a = bb(i32x4::from_array([1, -2, 3, -4])); let b = bb(i32x4::splat(3));
    println!("{:?} {:?} {:?} {:?}", a + b, a * b, a / b, a % b);
    println!("{:?} {:?}", (a.simd_lt(b)).to_bitmask(), a.simd_eq(i32x4::splat(3)).to_array());
    let x = bb(u8x16::from_array([0, 1, 2, 3, 250, 251, 252, 253, 4, 5, 6, 7, 8, 9, 10, 11]));
    println!("{:?} {:#x}", x.saturating_add(u8x16::splat(9)), x.simd_gt(u8x16::splat(100)).to_bitmask());
    println!("{:?}", simd_swizzle!(a, b, [7, 0, 5, 2]));
    println!("{} {} {} {}", a.reduce_sum(), a.reduce_max(), a.reduce_and(), x.reduce_or());
    let f = bb(f32x4::from_array([1.5, -0.25, 3.0, 8.0])); let g = bb(f64x2::from_array([0.5, 2.0]));
    println!("{:?} {:?} {} {:?}", f * f32x4::splat(2.0), -f, f.reduce_sum(), g / g);
    println!("{:?} {:?}", f.cast::<i32>(), a.cast::<f64>());
    let mask = a.simd_gt(i32x4::splat(0)); println!("{:?}", mask.select(a, b));
}
