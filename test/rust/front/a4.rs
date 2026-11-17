// rf: overflow-checks
#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

#[inline(never)]
fn add(a: i32, b: i32) -> i32 { a + b }
#[inline(never)]
fn mul8(a: u8, b: u8) -> u8 { a * b - 1 }
#[inline(never)]
fn neg(a: i32) -> i32 { -a }
#[inline(never)]
fn div(a: i32, b: i32) -> i32 { a / b + a % b }
#[inline(never)]
fn shl(a: u32, b: u32) -> u32 { (a << b) >> 1 }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    add(1, 2) + mul8(3, 4) as i32 + neg(5) + div(7, 2) + shl(1, 3) as i32
}
