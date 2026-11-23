/* oi's C API: Arbace's own clean-room <caml/shared_heap.h> (test/ocaml/capi/include/caml/mlvalues.h's notes; queue 523): the
   colours of a header (bits 8 and 9), NOT_MARKABLE the one of a block outside the heap, which no collector scans or frees. */
#ifndef CAML_SHARED_HEAP_H
#define CAML_SHARED_HEAP_H
#include "mlvalues.h"
#include "misc.h"
#define HEADER_COLOR_SHIFT 8
enum { NOT_MARKABLE = 3 << HEADER_COLOR_SHIFT };
#endif
