// rsi's sample of panics (doc/rust/EVAL.md §4): caught by catch_unwind (rustc's __rust_try, on the native-std leg the native
// cx_rust_try) with their messages and payloads, overflow checks, a panic through several lowered frames; no Drop that
// prints runs while unwinding (the native-std leg's class native-unwind).
use std::hint::black_box as bb;
use std::panic;

fn deep(n: u32) -> u32 { if n == 0 { let v: Vec<u32> = Vec::new(); v[bb(7)] } else { deep(n - 1) + 1 } }

fn main() {
    panic::set_hook(Box::new(|i| println!("hook: {}", i.payload().downcast_ref::<&str>().map(|s| s.to_string())
        .or(i.payload().downcast_ref::<String>().cloned()).unwrap_or_default())));
    let r = panic::catch_unwind(|| deep(bb(5)));
    println!("deep: {}", r.is_err());
    let r = panic::catch_unwind(|| { let x: u8 = bb(250); x + bb(10) });
    println!("overflow: {:?}", r.map_err(|e| e.downcast_ref::<&str>().map(|s| s.to_string())));
    let r = panic::catch_unwind(|| -> i32 { panic!("custom {}", bb(42)) });
    println!("custom: {:?}", r.map_err(|e| e.downcast::<String>().map(|s| *s).ok()));
    let r: Result<(), _> = panic::catch_unwind(|| panic::panic_any(bb(7u64)));
    println!("any: {:?}", r.map_err(|e| e.downcast::<u64>().map(|v| *v).ok()));
    let n = panic::catch_unwind(|| panic::catch_unwind(|| -> u8 { panic!("inner") }).is_err()).unwrap();
    println!("nested: {}", n);
    let _ = panic::take_hook();
    let v: Option<u8> = bb(None);
    std::process::exit(v.map(|x| x as i32).unwrap_or(3));
}
