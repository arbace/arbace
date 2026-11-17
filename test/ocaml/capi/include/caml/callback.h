/* oi's C API: Arbace's own clean-room <caml/callback.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_CALLBACK_H
#define CAML_CALLBACK_H
#include "mlvalues.h"
#include "memory.h"
CAMLextern value caml_callback(value closure, value arg);
CAMLextern value caml_callback2(value closure, value arg1, value arg2);
CAMLextern value caml_callback3(value closure, value arg1, value arg2, value arg3);
CAMLextern value caml_callbackN(value closure, int narg, value args[]);
CAMLextern value caml_callback_exn(value closure, value arg);
CAMLextern value caml_callback2_exn(value closure, value arg1, value arg2);
CAMLextern value caml_callback3_exn(value closure, value arg1, value arg2, value arg3);
CAMLextern value caml_callbackN_exn(value closure, int narg, value args[]);
CAMLextern const value *caml_named_value(const char *name);
typedef void (*caml_named_action)(const value *, char *);
CAMLextern void caml_iterate_named_values(caml_named_action);
CAMLextern void caml_main(char **argv);
CAMLextern void caml_startup(char **argv);
CAMLextern void caml_shutdown(void);
#endif
