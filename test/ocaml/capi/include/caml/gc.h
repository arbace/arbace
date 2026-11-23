/* oi's C API: Arbace's own clean-room <caml/gc.h> (test/ocaml/capi/include/caml/mlvalues.h's notes; queue 523): the header's
   making, its colour pre-shifted (mlvalues.h's Make_header), the reserved bits above the size unused here. */
#ifndef CAML_GC_H
#define CAML_GC_H
#include "mlvalues.h"
#define Make_header_with_reserved(wosize, tag, color, reserved) Make_header((wosize), (tag), (color))
#endif
