#![feature(lang_items)]
#![no_std]
#![no_main]
#![allow(internal_features)]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }
#[lang = "eh_personality"]
fn eh_personality() {}

struct D(i32);
impl Drop for D { fn drop(&mut self) { sink(self.0); } }

#[inline(never)]
fn sink(x: i32) { unsafe { core::ptr::write_volatile(&raw mut SINK, x) } }
static mut SINK: i32 = 0;
#[inline(never)]
fn may_panic(x: i32) -> i32 { if x > 100 { panic!() } x }

#[inline(never)]
fn f(a: i32) -> i32 { let d = D(a); let r = may_panic(a); d.0 + r }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 { f(3) }
