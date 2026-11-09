/* oi's C API: Arbace's own clean-room <caml/signals.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_SIGNALS_H
#define CAML_SIGNALS_H
#include "mlvalues.h"
CAMLextern void caml_enter_blocking_section(void);
CAMLextern void caml_enter_blocking_section_no_pending(void);
CAMLextern void caml_leave_blocking_section(void);
CAMLextern void caml_process_pending_actions(void);
CAMLextern value caml_process_pending_actions_exn(void);
CAMLextern int caml_check_pending_actions(void);
CAMLextern int caml_convert_signal_number(int);
CAMLextern int caml_rev_convert_signal_number(int);
#endif
