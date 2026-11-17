// RIND5's unit case (doc/rust/FRONTEND-SURVEY.md, "As built: RIND5's swizzle"): core's macro 2.0 simd_swizzle!, whose block
// holds `use $crate::simd::Swizzle; struct Impl; impl Swizzle<{$index.len()}> for Impl { .. }` and calls
// `Impl::concat_swizzle(a, b)`: the block's items and use in scope of the body, the const argument evaluated (4)
#![feature(portable_simd)]
use std::simd::prelude::*;
use std::simd::{simd_swizzle};
fn main() {
    let a = i32x4::from_array([1, -2, 3, -4]); let b = i32x4::splat(3);
    println!("{:?}", simd_swizzle!(a, b, [7, 0, 5, 2]));
}
