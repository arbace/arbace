// rsi's sample of the lowering's Rust delta (doc/rust/EVAL.md §5): the intrinsics rustc's -O0 IR calls that clang's C++
// rarely does, each at several widths, printed so that the lowered build's output is compared with the original's.
use std::hint::black_box as bb;

fn main() {
    // saturating arithmetic (llvm.{u,s}{add,sub}.sat)
    println!("{} {} {} {}", bb(250u8).saturating_add(bb(10)), bb(5u8).saturating_sub(bb(10)), bb(u64::MAX - 1).saturating_add(bb(7)), bb(3u64).saturating_sub(bb(9)));
    println!("{} {} {} {}", bb(120i8).saturating_add(bb(10)), bb(-120i8).saturating_sub(bb(10)), bb(i64::MIN + 2).saturating_sub(bb(7)), bb(-5i32).saturating_add(bb(-7)));
    println!("{} {}", bb(i128::MAX).saturating_add(bb(1)), bb(1u128).saturating_sub(bb(2)));
    // funnel shifts (rotates)
    println!("{:x} {:x} {:x} {:x}", bb(0x1234_5678_9abc_def0u64).rotate_left(bb(12)), bb(0x8001u16).rotate_right(bb(3)), bb(0xf0u8).rotate_left(bb(9)), bb(1u128).rotate_right(bb(1)));
    println!("{:x} {:x}", bb(0xdead_beefu32).rotate_left(bb(0)), bb(0xdead_beefu32).rotate_right(bb(32)));
    // three-way compares (llvm.{s,u}cmp)
    println!("{:?} {:?} {:?}", bb(3i64).cmp(&bb(-4)), bb(3u32).cmp(&bb(3)), bb(1u64).cmp(&bb(u64::MAX)));
    let mut v = vec![5i32, -3, 9, 0, -3, 12]; v.sort(); println!("{:?}", v);
    // bit reversal, byte swaps, counts
    println!("{:x} {:x} {:x} {:x}", bb(0x1u8).reverse_bits(), bb(0x1234u16).reverse_bits(), bb(0x1u64).reverse_bits(), bb(3u128).reverse_bits());
    println!("{:x} {:x} {:x}", bb(0x0102_0304u32).swap_bytes(), bb(0x0102_0304_0506_0708_090a_0b0c_0d0e_0f10u128).swap_bytes(), bb(0xabcdu16).swap_bytes());
    println!("{} {} {} {} {}", bb(1u8).leading_zeros(), bb(0x10u16).trailing_zeros(), bb(0u32).leading_zeros(), bb(1u128 << 100).leading_zeros(), bb(u128::MAX).count_ones());
    // saturating float-to-int casts (Rust's `as`)
    let fs = [f64::NAN, -1.5, -0.5, 0.0, 1e3, 300.7, -300.2, 1e30, -1e30, f64::INFINITY];
    for f in fs { print!("{} {} {} {} {} {}; ", bb(f) as u8, bb(f) as i8, bb(f) as i32, bb(f) as u64, bb(f) as i128, bb(f) as u128); }
    println!();
    for f in [f32::NAN, -129.0f32, 255.5, 4e9, -3e38] { print!("{} {} {} {}; ", bb(f) as u8, bb(f) as i16, bb(f) as u32, bb(f) as i64); }
    println!();
    // powi, min/max
    println!("{} {} {} {}", bb(2.0f64).powi(bb(10)), bb(1.5f32).powi(bb(-2)), bb(3.0f64).min(bb(f64::NAN)), bb(f32::NAN).max(bb(-1.0)));
    // i128 arithmetic
    let a = bb(170141183460469231731687303715884105727i128); let b = bb(-12345678901234567890123456789i128);
    println!("{} {} {} {} {}", a / b, a % b, a.wrapping_mul(b), (a as u128) >> 77, b.checked_mul(b).is_none());
    println!("{:?} {:?}", bb(u128::MAX).checked_add(1), bb(7u128).checked_mul(9));
}
