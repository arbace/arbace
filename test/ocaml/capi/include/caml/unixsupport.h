/* oi's C API: Arbace's own clean-room <caml/unixsupport.h> (test/ocaml/capi/include/caml/mlvalues.h's notes), the Unix library's
   header for its stubs as OCaml's manual and the library's interface (unix.mli: Unix_error of error * string * string) name it:
   Nothing, the error's raisers and the close-on-exec default. Declared only: a stub that calls a raiser is refused at its link
   (oi's Unix library is Go's, go/arbace/oi's ext_unix; lib-unix/common/cloexec's fdstatus_aux.c calls them only under _WIN32). */
#ifndef CAML_UNIXSUPPORT_H
#define CAML_UNIXSUPPORT_H
#include "mlvalues.h"
#define Nothing ((value) 0)
CAMLextern void caml_unix_error(int errcode, const char *cmdname, value arg);
CAMLextern void caml_uerror(const char *cmdname, value arg);
CAMLextern void caml_unix_check_path(value path, const char *cmdname);
CAMLextern int caml_unix_cloexec_default;
CAMLextern int caml_unix_cloexec_p(value cloexec);
#define unix_error caml_unix_error
#define uerror caml_uerror
#endif
