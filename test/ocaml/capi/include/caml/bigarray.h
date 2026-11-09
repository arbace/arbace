/* oi's C API: Arbace's own clean-room <caml/bigarray.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_BIGARRAY_H
#define CAML_BIGARRAY_H
#include "mlvalues.h"
#include <stdarg.h>
enum caml_ba_kind {
  CAML_BA_FLOAT32, CAML_BA_FLOAT64, CAML_BA_SINT8, CAML_BA_UINT8, CAML_BA_SINT16, CAML_BA_UINT16, CAML_BA_INT32,
  CAML_BA_INT64, CAML_BA_CAML_INT, CAML_BA_NATIVE_INT, CAML_BA_COMPLEX32, CAML_BA_COMPLEX64, CAML_BA_CHAR,
  CAML_BA_FLOAT16, CAML_BA_FIRST_UNIMPLEMENTED_KIND, CAML_BA_KIND_MASK = 0xFF
};
enum caml_ba_layout { CAML_BA_C_LAYOUT = 0, CAML_BA_FORTRAN_LAYOUT = 0x100, CAML_BA_LAYOUT_MASK = 0x100 };
enum caml_ba_managed { CAML_BA_EXTERNAL = 0, CAML_BA_MANAGED = 0x200, CAML_BA_MAPPED_FILE = 0x400, CAML_BA_MANAGED_MASK = 0x600 };
enum caml_ba_subarray { CAML_BA_SUBARRAY = 0x800 };
#define CAML_BA_MAX_NUM_DIMS 16
struct caml_ba_proxy { intnat refcount; void *data; uintnat size; };
struct caml_ba_array {
  void *data;
  intnat num_dims;
  intnat flags;
  struct caml_ba_proxy *proxy;
  intnat dim[];
};
#define Caml_ba_array_val(v) ((struct caml_ba_array *) Data_custom_val(v))
#define Caml_ba_data_val(v) (Caml_ba_array_val(v)->data)
#define Caml_ba_layout_val(v) (Caml_ba_array_val(v)->flags & CAML_BA_LAYOUT_MASK)
#define Caml_ba_kind_val(v) (Caml_ba_array_val(v)->flags & CAML_BA_KIND_MASK)
CAMLextern int caml_ba_element_size[];
CAMLextern uintnat caml_ba_num_elts(struct caml_ba_array *b);
CAMLextern uintnat caml_ba_byte_size(struct caml_ba_array *b);
CAMLextern value caml_ba_alloc(int flags, int num_dims, void *data, intnat *dim);
CAMLextern value caml_ba_alloc_dims(int flags, int num_dims, void *data, ...);
#endif
