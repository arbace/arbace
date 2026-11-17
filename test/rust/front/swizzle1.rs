// RIND5's unit case (doc/rust/FRONTEND-SURVEY.md, "As built: RIND5's swizzle"): core's macro 2.0 simd_swizzle!, whose block
// holds `use $crate::simd::Swizzle; struct Impl; impl Swizzle<{$index.len()}> for Impl { .. }` and calls
// `Impl::concat_swizzle(a, b)`: the block's items and use in scope of the body, the const argument evaluated (4); and a generic
// associated type normalized with its own argument (`f.cast::<i32>()`: `<Simd<f32, 4> as SimdFloat>::Cast<i32>`, Simd<i32, 4>)
#![feature(portable_simd)]
use std::simd::prelude::*;
use std::simd::{simd_swizzle};
fn main() {
    let a = i32x4::from_array([1, -2, 3, -4]); let b = i32x4::splat(3);
    println!("{:?}", simd_swizzle!(a, b, [7, 0, 5, 2]));
    let f = f32x4::splat(1.5); println!("{:?}", f.cast::<i32>());
}
