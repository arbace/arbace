/* oi's C API: Arbace's own clean-room <caml/hash.h> (test/ocaml/capi/include/caml/mlvalues.h's notes; queue 547, base's
   internalhash_stubs.c and hash_stubs.c). The mixing functions of OCaml's structural hash, the MurmurHash3 steps the manual's
   Hashtbl.hash names, as oi's own go/arbace/oi/hash.ae computes them: a stub's mix and oi's Hashtbl.hash agree. */
#ifndef CAML_HASH_H
#define CAML_HASH_H
#include "mlvalues.h"
CAMLextern uint32_t caml_hash_mix_uint32(uint32_t h, uint32_t d);
CAMLextern uint32_t caml_hash_mix_intnat(uint32_t h, intnat d);
CAMLextern uint32_t caml_hash_mix_int64(uint32_t h, int64_t d);
CAMLextern uint32_t caml_hash_mix_double(uint32_t h, double d);
CAMLextern uint32_t caml_hash_mix_float(uint32_t h, float d);
CAMLextern uint32_t caml_hash_mix_string(uint32_t h, value s);
#endif
