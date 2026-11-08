/* oi's C API: Arbace's own clean-room <caml/intext.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_INTEXT_H
#define CAML_INTEXT_H
#include "mlvalues.h"
#include "custom.h"
CAMLextern intnat caml_output_value_to_block(value v, value flags, char *data, intnat len);
CAMLextern value caml_input_value_from_block(const char *data, intnat len);
CAMLextern value caml_output_value_to_string(value v, value flags);
CAMLextern value caml_input_val_from_bytes(value str, intnat ofs);
CAMLextern void caml_serialize_int_1(int i);
CAMLextern void caml_serialize_int_2(int i);
CAMLextern void caml_serialize_int_4(int32_t i);
CAMLextern void caml_serialize_int_8(int64_t i);
CAMLextern void caml_serialize_float_4(float f);
CAMLextern void caml_serialize_float_8(double f);
CAMLextern void caml_serialize_block_1(void *data, intnat len);
CAMLextern int caml_deserialize_uint_1(void);
CAMLextern int caml_deserialize_sint_1(void);
CAMLextern int caml_deserialize_uint_2(void);
CAMLextern int caml_deserialize_sint_2(void);
CAMLextern uint32_t caml_deserialize_uint_4(void);
CAMLextern int32_t caml_deserialize_sint_4(void);
CAMLextern uint64_t caml_deserialize_uint_8(void);
CAMLextern int64_t caml_deserialize_sint_8(void);
CAMLextern void caml_deserialize_block_1(void *data, intnat len);
CAMLextern void caml_deserialize_error(char *msg);
#endif
