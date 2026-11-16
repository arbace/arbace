#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

#[derive(Clone, Copy)] struct S12 { a: i32, b: i32, c: i32 }
#[derive(Clone, Copy)] struct S16 { a: i64, b: i64 }
#[derive(Clone, Copy)] struct S24 { a: i64, b: i64, c: i64 }
#[inline(never)] fn s12(x: S12) -> S12 { S12 { a: x.b, b: x.c, c: x.a } }
#[inline(never)] fn s16(x: S16) -> S16 { S16 { a: x.b, b: x.a } }
#[inline(never)] fn s24(x: S24) -> S24 { let y = x; S24 { a: y.c, b: y.b, c: y.a } }
#[inline(never)] fn copy24(p: &S24) -> S24 { *p }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    let a = s12(S12 { a: 1, b: 2, c: 3 });
    let b = s16(S16 { a: 4, b: 5 });
    let c = s24(S24 { a: 6, b: 7, c: 8 });
    let d = copy24(&c);
    a.a + b.a as i32 + c.a as i32 + d.c as i32
}
