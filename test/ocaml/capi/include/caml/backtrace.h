/* oi's C API: Arbace's own clean-room <caml/backtrace.h> (queue 547, base's exn_stubs.c). oi records backtraces on its own side
   (go/arbace/oi); the two names a stub reads or writes are words of the C runtime here, which oi never sets: clearing the position
   changes nothing oi records, and caml_backtrace_last_exn is never a raised exception (Base's Exn.is_phys_equal_most_recent is
   false, so Base raises again without the original backtrace). */
#ifndef CAML_BACKTRACE_H
#define CAML_BACKTRACE_H
#include "mlvalues.h"
#ifdef CAML_INTERNALS
CAMLextern intnat caml_backtrace_pos;
CAMLextern value caml_backtrace_last_exn;
#endif
#endif
