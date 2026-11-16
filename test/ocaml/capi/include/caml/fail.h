/* oi's C API: Arbace's own clean-room <caml/fail.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_FAIL_H
#define CAML_FAIL_H
#include "mlvalues.h"
CAMLextern void caml_raise(value bucket) __attribute__((noreturn));
CAMLextern void caml_raise_constant(value tag) __attribute__((noreturn));
CAMLextern void caml_raise_with_arg(value tag, value arg) __attribute__((noreturn));
CAMLextern void caml_raise_with_args(value tag, int nargs, value arg[]) __attribute__((noreturn));
CAMLextern void caml_raise_with_string(value tag, const char *msg) __attribute__((noreturn));
CAMLextern void caml_failwith(const char *msg) __attribute__((noreturn));
CAMLextern void caml_failwith_value(value msg) __attribute__((noreturn));
CAMLextern void caml_invalid_argument(const char *msg) __attribute__((noreturn));
CAMLextern void caml_invalid_argument_value(value msg) __attribute__((noreturn));
CAMLextern void caml_raise_out_of_memory(void) __attribute__((noreturn));
CAMLextern void caml_raise_stack_overflow(void) __attribute__((noreturn));
CAMLextern void caml_raise_sys_error(value) __attribute__((noreturn));
CAMLextern void caml_raise_end_of_file(void) __attribute__((noreturn));
CAMLextern void caml_raise_zero_divide(void) __attribute__((noreturn));
CAMLextern void caml_raise_not_found(void) __attribute__((noreturn));
CAMLextern void caml_array_bound_error(void) __attribute__((noreturn));
CAMLextern void caml_raise_sys_blocked_io(void) __attribute__((noreturn));
#endif
