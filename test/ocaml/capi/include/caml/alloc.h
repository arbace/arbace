/* oi's C API: Arbace's own clean-room <caml/alloc.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_ALLOC_H
#define CAML_ALLOC_H
#include "mlvalues.h"
#include "memory.h"
CAMLextern value caml_alloc(mlsize_t wosize, tag_t);
CAMLextern value caml_alloc_tuple(mlsize_t);
CAMLextern value caml_alloc_float_array(mlsize_t);
CAMLextern value caml_alloc_string(mlsize_t len);
CAMLextern value caml_alloc_initialized_string(mlsize_t len, const char *);
CAMLextern value caml_copy_string(const char *);
CAMLextern value caml_copy_string_array(const char **);
CAMLextern value caml_copy_double(double);
CAMLextern value caml_copy_int32(int32_t);
CAMLextern value caml_copy_int64(int64_t);
CAMLextern value caml_copy_nativeint(intnat);
CAMLextern value caml_alloc_array(value (*funct)(const char *), const char **array);
CAMLextern value caml_alloc_sprintf(const char *format, ...);
CAMLextern value caml_alloc_some(value);
CAMLextern value caml_alloc_record(tag_t, mlsize_t);
typedef void (*final_fun)(value);
CAMLextern value caml_alloc_final(mlsize_t, final_fun, mlsize_t, mlsize_t);
CAMLextern int caml_convert_flag_list(value, const int *);
#endif
