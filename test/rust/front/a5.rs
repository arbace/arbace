#![no_std]
#![no_main]
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }

enum E { A, B(i32), C { x: u8, y: u16 } }
enum Color { Red = 1, Green = 5, Blue }
enum Opt { N, S(i32) }
#[inline(never)]
fn val(e: &E) -> i32 { match e { E::A => 1, E::B(n) => *n, E::C { x, y } => *x as i32 + *y as i32 } }
#[inline(never)]
fn mk(n: i32) -> E { if n > 3 { E::B(n) } else if n > 1 { E::C { x: 1, y: 2 } } else { E::A } }
#[inline(never)]
fn col(c: Color) -> i32 { match c { Color::Red => 10, Color::Green => 20, Color::Blue => 30 } }
#[inline(never)]
fn some(n: i32) -> Opt { if n > 0 { Opt::S(n) } else { Opt::N } }
#[inline(never)]
fn unwrap(o: Opt) -> i32 { match o { Opt::S(v) => v, Opt::N => -1 } }
#[inline(never)]
fn refopt(p: Option<&i32>) -> i32 { match p { Some(v) => *v, None => 0 } }

#[unsafe(no_mangle)]
pub extern "C" fn main(_argc: i32, _argv: *const *const u8) -> i32 {
    let e = mk(5);
    let x = 7;
    val(&e) + col(Color::Blue) + unwrap(some(3)) + refopt(Some(&x)) + refopt(None)
}
