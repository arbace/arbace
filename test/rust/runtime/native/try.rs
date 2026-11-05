// catch_unwind's frame for RE2's native-std leg (doc/rust/EVAL.md §4): rustc's internal __rust_try, which a lowered program
// calls in its place (arbace.rust.lower), compiled natively by the host's rustc so that its landing pad catches the native
// unwinder's panics that pass through the lowered frames. The same contract as core::intrinsics::catch_unwind: try_fn(data);
// on a panic catch_fn(data, payload) and true, else false.
#![feature(core_intrinsics)]
#![no_std]
#![allow(internal_features, improper_ctypes_definitions)]

#[unsafe(no_mangle)]
pub unsafe extern "C-unwind" fn cx_rust_try(try_fn: fn(*mut u8), data: *mut u8, catch_fn: fn(*mut u8, *mut u8)) -> bool {
    unsafe { core::intrinsics::catch_unwind(try_fn, data, catch_fn) }
}
