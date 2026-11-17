#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

// RFP33 (a) (doc/rust/FRONTEND-SURVEY.md, "As built: the rest of RFP28-RFP41"): the pass judge's opaque types, two
// functions returning `impl Fn(i32) -> i32` (an opaque type each, alike but for their DefIds) and a generic definer, whose
// hidden type takes the call's arguments
#[inline(never)] fn adder(n: i32) -> impl Fn(i32) -> i32 { move |x| x + n }
#[inline(never)] fn scaler(n: i32) -> impl Fn(i32) -> i32 { move |x| x * n }
#[inline(never)] fn twice<F: Fn(i32) -> i32>(f: F) -> impl Fn(i32) -> i32 { move |x| f(f(x)) }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    let a = adder(3);
    let s = scaler(2);
    let t = twice(adder(1));
    a(1) + s(5) + t(10)
}
