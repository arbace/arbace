/* oi's C API: Arbace's own clean-room <caml/misc.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_MISC_H
#define CAML_MISC_H
#include "mlvalues.h"
#include <stdarg.h>
CAMLextern void caml_fatal_error(const char *, ...) __attribute__((noreturn));
#define CAMLuninitialized
#endif
