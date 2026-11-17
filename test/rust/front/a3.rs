#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

#[derive(Clone, Copy)]
struct P { x: i32, y: i32 }
struct Q { a: u8, b: i64, c: u8 }

#[inline(never)]
fn mk(x: i32, y: i32) -> P { P { x, y } }
#[inline(never)]
fn sum(p: P) -> i32 { p.x + p.y }
#[inline(never)]
fn swap(t: (i32, u8)) -> (u8, i32) { (t.1, t.0) }
#[inline(never)]
fn q(a: u8, b: i64) -> i64 { let q = Q { a, b, c: 3 }; q.b + q.a as i64 + q.c as i64 }
#[inline(never)]
fn bump(p: &mut P) { p.x += 1; p.y = p.x * 2; }
#[inline(never)]
fn arr(i: usize) -> i32 { let a = [1, 2, 3, 4]; a[i & 3] }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    let mut p = mk(3, 4);
    bump(&mut p);
    let s = sum(p);
    let t = swap((s, 9));
    s + t.0 as i32 + t.1 + q(1, 2) as i32 + arr(5)
}
