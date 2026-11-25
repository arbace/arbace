/* oi's C API: Arbace's own clean-room <caml/misc.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_MISC_H
#define CAML_MISC_H
#include "mlvalues.h"
#include <stdarg.h>
CAMLextern void caml_fatal_error(const char *, ...) __attribute__((noreturn));
#define CAMLuninitialized
/* the system's characters and a C program's own main (queue 539: embedded/cmcaml's main_os) */
#ifndef CAML_CHAR_OS_DEFINED
#define CAML_CHAR_OS_DEFINED
typedef char char_os;
#endif
#ifdef CAML_INTERNALS
#define main_os main
#endif
#endif
