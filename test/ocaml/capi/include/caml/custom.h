/* oi's C API: Arbace's own clean-room <caml/custom.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_CUSTOM_H
#define CAML_CUSTOM_H
#include "mlvalues.h"
struct custom_fixed_length { intnat bsize_32; intnat bsize_64; };
struct custom_operations {
  const char *identifier;
  void (*finalize)(value v);
  int (*compare)(value v1, value v2);
  intnat (*hash)(value v);
  void (*serialize)(value v, uintnat *bsize_32, uintnat *bsize_64);
  uintnat (*deserialize)(void *dst);
  int (*compare_ext)(value v1, value v2);
  const struct custom_fixed_length *fixed_length;
};
#define custom_finalize_default NULL
#define custom_compare_default NULL
#define custom_hash_default NULL
#define custom_serialize_default NULL
#define custom_deserialize_default NULL
#define custom_compare_ext_default NULL
#define custom_fixed_length_default NULL
#define Custom_ops_val(v) (*((const struct custom_operations **) (v)))
CAMLextern value caml_alloc_custom(const struct custom_operations *ops, uintnat size, mlsize_t mem, mlsize_t max);
CAMLextern value caml_alloc_custom_mem(const struct custom_operations *ops, uintnat size, mlsize_t mem);
CAMLextern value caml_alloc_custom_dep(const struct custom_operations *ops, uintnat size, mlsize_t mem);
CAMLextern void caml_register_custom_operations(const struct custom_operations *ops);
#endif
