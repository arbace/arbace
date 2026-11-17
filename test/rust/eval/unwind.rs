// rsi's sample of unwinding (RE3, doc/rust/EVAL.md §8): drops while a panic unwinds lowered frames, catch_unwind at several depths,
// a panic's payload resumed, a panic in a thread's... (none: threads are a class); last a panic that leaves main: its message on
// stderr and the status 101.
use std::panic;

struct Noisy(&'static str);
impl Drop for Noisy {
    fn drop(&mut self) { println!("drop {}", self.0); }
}

fn deep(n: u32) -> u32 {
    let _g = Noisy(if n % 2 == 0 { "even" } else { "odd" });
    if n == 0 { panic!("bottom reached"); }
    deep(n - 1) + 1
}

fn main() {
    panic::set_hook(Box::new(|info| {
        let msg = info.payload().downcast_ref::<&str>().map(|s| s.to_string())
            .or_else(|| info.payload().downcast_ref::<String>().cloned()).unwrap_or_default();
        println!("hook: {}", msg);
    }));
    let r = panic::catch_unwind(|| deep(3));
    println!("caught: {:?}", r.is_err());
    let v = vec![Noisy("v1"), Noisy("v2")];
    let r = panic::catch_unwind(panic::AssertUnwindSafe(|| {
        let _a = Noisy("a");
        let w: Vec<u8> = Vec::new();
        let _ = w[3];
    }));
    println!("index: {:?}", r.err().and_then(|e| e.downcast_ref::<String>().cloned()));
    drop(v);
    let r = panic::catch_unwind(|| {
        let inner = panic::catch_unwind(|| { let _b = Noisy("b"); panic!("inner {}", 7) });
        println!("inner caught {}", inner.is_err());
        let _c = Noisy("c");
        panic::resume_unwind(Box::new(42u32));
    });
    println!("resumed: {:?}", r.err().and_then(|e| e.downcast_ref::<u32>().copied()));
    let r: Result<i32, _> = panic::catch_unwind(|| { let x: Option<i32> = None; x.expect("nothing there") });
    println!("expect: {}", r.is_err());
    let _ = panic::take_hook();
    let _last = Noisy("last");
    let n: u8 = std::hint::black_box(200);
    let m = n.checked_add(100).unwrap_or_else(|| panic!("overflow of {}", n));
    println!("unreachable {}", m);
}
