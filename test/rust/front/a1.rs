#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

#[inline(never)]
fn add(a: i32, b: i32) -> i32 { a + b }

#[inline(never)]
fn fib(n: u32) -> u32 { if n < 2 { n } else { fib(n - 1) + fib(n - 2) } }

fn sum_to(n: u32) -> u32 {
    let mut s = 0;
    let mut i = 0;
    while i < n { s += i; i += 1; }
    s
}

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    let x = add(2, 3);
    let y = fib(10) + sum_to(10);
    (x as u32 + y) as i32 - 100
}
