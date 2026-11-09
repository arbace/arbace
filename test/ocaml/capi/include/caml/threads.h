/* oi's C API: Arbace's own clean-room <caml/threads.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_THREADS_H
#define CAML_THREADS_H
#include "mlvalues.h"
#include "signals.h"
#define caml_release_runtime_system caml_enter_blocking_section
#define caml_acquire_runtime_system caml_leave_blocking_section
CAMLextern int caml_c_thread_register(void);
CAMLextern int caml_c_thread_unregister(void);
#endif
