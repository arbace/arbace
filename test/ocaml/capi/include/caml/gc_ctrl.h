/* oi's C API: Arbace's own clean-room <caml/gc_ctrl.h> (test/ocaml/capi/include/caml/mlvalues.h's notes; queue 523). Under
   CAML_INTERNALS the runtime's statistics a stub may read, live: each a read of oi's counter (go/arbace/oi's GcCount, Gc.quick_stat's
   values), which the stubs' own allocations move (a minor heap's worth of words ends a cycle of the simulated major heap). */
#ifndef CAML_GC_CTRL_H
#define CAML_GC_CTRL_H

#ifdef CAML_INTERNALS

#include "misc.h"

CAMLextern uintnat caml_max_stack_wsize;
CAMLextern uintnat caml_fiber_wsz;
CAMLextern uintnat caml_oi_gc_count(int);

#define caml_major_cycles_completed (caml_oi_gc_count(0))
#define caml_stat_major_collections caml_major_cycles_completed
#define caml_stat_heap_wsz (caml_oi_gc_count(1))
#define caml_stat_top_heap_wsz (caml_oi_gc_count(2))
#define caml_stat_heap_chunks (caml_oi_gc_count(3))
#define caml_stat_compactions 0
#define caml_stat_minor_words ((double) caml_oi_gc_count(4))
#define caml_stat_promoted_words ((double) caml_oi_gc_count(5))
#define caml_stat_major_words ((double) caml_oi_gc_count(6))
#define caml_allocated_words (caml_oi_gc_count(4) - caml_oi_gc_count(5) + caml_oi_gc_count(6))

#endif /* CAML_INTERNALS */

#endif /* CAML_GC_CTRL_H */
