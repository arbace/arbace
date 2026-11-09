/* oi's C API: Arbace's own clean-room <caml/memory.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_MEMORY_H
#define CAML_MEMORY_H
#include "mlvalues.h"
/* No collector moves a stub's values (oi keeps the copies in ci's memory in place for the call), so the local roots are plain
   variables and the registration of global roots records nothing. */
#define CAMLparam0() ((void) 0)
#define CAMLparam1(x) ((void) (x))
#define CAMLparam2(x, y) ((void) (x), (void) (y))
#define CAMLparam3(x, y, z) ((void) (x), (void) (y), (void) (z))
#define CAMLparam4(x, y, z, t) ((void) (x), (void) (y), (void) (z), (void) (t))
#define CAMLparam5(x, y, z, t, u) ((void) (x), (void) (y), (void) (z), (void) (t), (void) (u))
#define CAMLparamN(x, size) ((void) (x), (void) (size))
#define CAMLxparam1(x) ((void) (x))
#define CAMLxparam2(x, y) ((void) (x), (void) (y))
#define CAMLxparam3(x, y, z) ((void) (x), (void) (y), (void) (z))
#define CAMLxparam4(x, y, z, t) ((void) (x), (void) (y), (void) (z), (void) (t))
#define CAMLxparam5(x, y, z, t, u) ((void) (x), (void) (y), (void) (z), (void) (t), (void) (u))
#define CAMLxparamN(x, size) ((void) (x), (void) (size))
#define CAMLlocal1(x) value x = Val_unit
#define CAMLlocal2(x, y) value x = Val_unit, y = Val_unit
#define CAMLlocal3(x, y, z) value x = Val_unit, y = Val_unit, z = Val_unit
#define CAMLlocal4(x, y, z, t) value x = Val_unit, y = Val_unit, z = Val_unit, t = Val_unit
#define CAMLlocal5(x, y, z, t, u) value x = Val_unit, y = Val_unit, z = Val_unit, t = Val_unit, u = Val_unit
#define CAMLlocalN(x, size) value x[size] = { 0 }
#define CAMLreturn0 return
#define CAMLreturnT(type, result) return (result)
#define CAMLreturn(result) return (result)
#define CAMLdrop ((void) 0)
#define CAMLnoreturn ((void) 0)
#define Store_field(block, offset, val) ((void) (Field((block), (offset)) = (val)))
#define Begin_root(r) {
#define End_roots() }
CAMLextern void caml_modify(volatile value *, value);
CAMLextern void caml_initialize(volatile value *, value);
CAMLextern int caml_atomic_cas_field(value, intnat, value, value);
CAMLextern void caml_register_global_root(value *);
CAMLextern void caml_remove_global_root(value *);
CAMLextern void caml_register_generational_global_root(value *);
CAMLextern void caml_remove_generational_global_root(value *);
CAMLextern void caml_modify_generational_global_root(value *, value);
CAMLextern value caml_alloc_shr(mlsize_t, tag_t);
CAMLextern void *caml_stat_alloc(size_t);
CAMLextern void *caml_stat_alloc_noexc(size_t);
CAMLextern void *caml_stat_calloc_noexc(size_t, size_t);
CAMLextern void *caml_stat_resize(void *, size_t);
CAMLextern void caml_stat_free(void *);
CAMLextern char *caml_stat_strdup(const char *);
CAMLextern void caml_alloc_dependent_memory(value, mlsize_t);
CAMLextern void caml_free_dependent_memory(value, mlsize_t);
CAMLextern void caml_adjust_gc_speed(mlsize_t, mlsize_t);
#define Alloc_small(result, wosize, tag) ((result) = caml_alloc_small((wosize), (tag)))
CAMLextern value caml_alloc_small(mlsize_t, tag_t);
#endif
