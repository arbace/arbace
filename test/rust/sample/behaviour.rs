// repr/rust's behaviour case (bin/repr-rust test, case 9; doc/rust/RUST-NOTES.md, "RR5"): a
// program of the constructs RR5 met in rustc's UI suite, run from the original and from its
// printed files: attributes on operands and in fixed places, an empty binder, a closure's `_`
// type, a struct named union, a panic whose column mode :lines does not keep.
#![feature(stmt_expr_attributes)]
#![allow(non_camel_case_types, unused_assignments, unused_attributes)]

struct union;

impl union {
    fn new() -> Self {
        union {}
    }
}

type F = for<> fn(u8) -> u8;
type T = [u8; #[allow(unused)] 2];

#[repr(u8)]
enum E {
    A = #[allow(unused)] 1,
    B = 2,
}

static S: u8 = #[allow(unused)] 3;

fn twice(f: F, x: u8) -> u8 {
    f(f(x))
}

fn main() {
    let add = |x: _, y: u8| -> u8 { x + y };
    let v = [1u8, 2, 3][#[allow(unused)] 1];
    let w = #[allow(unused)] v
        * 2;
    let mut z = 0;
    #[allow(unused)]
    z = w;
    let r = (#[allow(unused)] || z + 1)();
    let s = Some(5)
        .map(|x| x
            + 1);
    let t: T = [E::A as u8, E::B as u8];
    let _ = union::new();
    println!("{} {} {} {} {:?} {} {:?} {}", add(1, 2), v, w, r, s, twice(|x| x + 1, 3), t, S);
    let caught = std::panic::catch_unwind(|| {
        let o: Option<u8> = None;
        1 +    o.unwrap()
    });
    println!("{}", caught.is_err());
}
