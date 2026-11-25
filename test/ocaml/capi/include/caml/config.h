/* oi's C API: Arbace's own clean-room <caml/config.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_CONFIG_H
#define CAML_CONFIG_H
#include "mlvalues.h"
#include <inttypes.h>
#define ARCH_SIXTYFOUR
#define SIZEOF_PTR 8
#define SIZEOF_LONG 8
#define SIZEOF_INT 4
/* the C types of OCaml's fixed-width integers on LP64 (queue 547, ocaml_intrinsics_kernel's int_stubs.c) */
#define ARCH_INT32_TYPE int
#define ARCH_UINT32_TYPE unsigned int
#define ARCH_INT64_TYPE long
#define ARCH_UINT64_TYPE unsigned long
/* printf's conversion of a uintnat (queue 530) */
#define CAML_PRIuNAT PRIuPTR
#endif
