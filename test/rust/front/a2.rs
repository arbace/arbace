#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

#[inline(never)]
fn f(a: u8, b: i64, c: bool) -> i64 {
    let x = a as i64 + b;
    let y = (b as u8) as u32 as i32;
    let z = !c;
    let w = if z && a > 3 { x ^ 5 } else { -(x & 7) };
    let q = (w << 2) | (w >> 1);
    let r = y as i64 - (a == 3) as i64;
    q + r
}

fn is_even(n: u32) -> bool { n % 2 == 0 }
fn tern(a: i32) -> i32 { match a { 0 => 10, 1 => 20, 5 => 50, _ => a } }
fn unit(p: &mut i32) { *p += 1; }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    let mut v = 7;
    unit(&mut v);
    let r = f(9, 100, true) as i32 + tern(5) + is_even(4) as i32 + v;
    r - 200
}
