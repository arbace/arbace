/* oi's C API (doc/ocaml/OCAML-SURVEY.md, "As built: the C API on ci"): Arbace's own clean-room <caml/mlvalues.h>, written
   from OCaml's manual (chapter 22, "Interfacing C with OCaml") for C stubs run by ci beside oi. A value is a word: an
   integer tagged by its low bit, or the address of a block's first field in ci's memory, its header the word before (wosize
   in bits 10 and up, the tag in the low 8). oi's values are Go's: a block a stub is given or returns is a copy in ci's memory
   that oi keeps in step at the boundary (go/arbace/oicapi), and no collector moves it, so the root macros register nothing. */
#ifndef CAML_MLVALUES_H
#define CAML_MLVALUES_H

#include <stddef.h>
#include <stdint.h>
/* (queue 547) what OCaml's <caml/mlvalues.h> includes on the way (misc.h's stdlib.h, stdarg.h and limits.h, domain_state.h's
   stdio.h), which a stub may rely on: ppx_expect's runtime stubs' fflush(stdout) */
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <limits.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef intptr_t intnat;
typedef uintptr_t uintnat;
typedef intnat value;
typedef uintnat header_t;
typedef uintnat mlsize_t;
typedef unsigned int tag_t;
typedef uintnat mark_t;
typedef int32_t int32;
typedef uint32_t uint32;
typedef int64_t int64;
typedef uint64_t uint64;
typedef double float64;

#define CAMLextern extern
#define CAMLprim
#define CAMLexport
#define CAMLlocal_export
/* a weak definition (queue 547: base's am_testing.c, which ppx_inline_test's runner overrides); ci links weak symbols */
#define CAMLweakdef __attribute__((weak))
#define CAMLnoret __attribute__((noreturn))
/* the older spelling, after the declarator (queue 547: jane-street-headers' ocaml_utils.h, `... ) Noreturn;`) */
#define Noreturn __attribute__((noreturn))
#define CAMLnoreturn_start
#define CAMLnoreturn_end __attribute__((noreturn))
#define CAMLunused_start
#define CAMLunused_end __attribute__((unused))
#define CAMLunused __attribute__((unused))
#define CAMLthread_local _Thread_local
#define CAMLalign(n) _Alignas(n)
#define CAMLassert(x) ((void) 0)
#define CAMLlikely(e) (e)
#define CAMLunlikely(e) (e)
#define Caml_inline static inline
#define Max_long ((intnat)(((uintnat)1 << (8 * sizeof(value) - 2)) - 1))
#define Min_long (-Max_long - 1)
#define Max_wosize (((uintnat)1 << 54) - 1)
#define Max_young_wosize 256
#define Max_young_whsize (Max_young_wosize + 1)

/* integers */
#define Val_long(x) ((intnat) (((uintnat)(x) << 1)) + 1)
#define Long_val(x) ((x) >> 1)
#define Val_int(x) Val_long(x)
#define Int_val(x) ((int) Long_val(x))
#define Unsigned_long_val(x) ((uintnat)(x) >> 1)
#define Unsigned_int_val(x) ((int) Unsigned_long_val(x))
#define Is_long(x) (((x) & 1) != 0)
#define Is_block(x) (((x) & 1) == 0)
#define Val_bool(x) Val_int((x) != 0)
#define Bool_val(x) Int_val(x)
#define Val_false Val_int(0)
#define Val_true Val_int(1)
#define Val_not(x) (Val_false + Val_true - (x))
#define Val_unit Val_int(0)
#define Val_emptylist Val_int(0)
#define Val_none Val_int(0)
#define Tag_cons 0
#define Tag_some 0
#define Some_val(v) Field(v, 0)
#define Is_none(v) ((v) == Val_none)
#define Is_some(v) Is_block(v)

/* headers and blocks */
#define Hd_val(v) (((header_t *) (v))[-1])
#define Hp_val(v) (((header_t *) (v)) - 1)
#define Val_hp(hp) ((value) (((header_t *) (hp)) + 1))
#define Wosize_hd(hd) ((mlsize_t) ((hd) >> 10))
#define Tag_hd(hd) ((tag_t) ((hd) & 0xFF))
#define Make_header(wosize, tag, color) ((header_t) (((header_t) (wosize) << 10) + (color) + (tag_t) (tag)))
#define Wosize_val(v) (Wosize_hd(Hd_val(v)))
#define Tag_val(v) (Tag_hd(Hd_val(v)))
#define Bosize_val(v) (Wosize_val(v) * sizeof(value))
#define Wsize_bsize(n) ((n) / sizeof(value))
#define Bsize_wsize(n) ((n) * sizeof(value))
#define Field(x, i) (((value *) (x))[i])
#define Op_val(x) ((value *) (x))
#define Bp_val(v) ((char *) (v))
#define Val_bp(p) ((value) (p))
#define Val_op(op) ((value) (op))

#define Forcing_tag 244
#define Cont_tag 245
#define Lazy_tag 246
#define Closure_tag 247
#define Object_tag 248
#define Infix_tag 249
#define Forward_tag 250
#define No_scan_tag 251
#define Abstract_tag 251
#define String_tag 252
#define Double_tag 253
#define Double_array_tag 254
#define Custom_tag 255

/* strings */
#define String_val(x) ((const char *) Bp_val(x))
#define Bytes_val(x) ((unsigned char *) Bp_val(x))
#define Byte(x, i) (((char *) (x)) [i])
#define Byte_u(x, i) (((unsigned char *) (x)) [i])
CAMLextern mlsize_t caml_string_length(value);
CAMLextern int caml_string_is_c_safe(value);

/* floats */
#define Double_wosize ((sizeof(double) / sizeof(value)))
#define Double_val(v) (* (double *) (v))
#define Store_double_val(v, d) (* (double *) (v) = (d))
#define Double_flat_field(v, i) Double_val((value) ((double *) (v) + (i)))
#define Store_double_flat_field(v, i, d) Store_double_val((value) ((double *) (v) + (i)), (d))
#define Double_field(v, i) Double_flat_field(v, i)
#define Store_double_field(v, i, d) Store_double_flat_field(v, i, d)
#define Double_array_tag_val(v) Double_array_tag
CAMLextern mlsize_t caml_array_length(value);
CAMLextern int caml_is_double_array(value);

/* custom blocks and the boxed integers */
#define Data_custom_val(v) ((void *) &Field((v), 1))
#define Int32_val(v) (*((int32_t *) Data_custom_val(v)))
#define Nativeint_val(v) (*((intnat *) Data_custom_val(v)))
#define Int64_val(v) (*((int64_t *) Data_custom_val(v)))
#define Data_abstract_val(v) ((void *) Op_val(v))

/* exception results (caml_callback_exn) */
#define Make_exception_result(v) ((v) | 2)
#define Is_exception_result(v) (((v) & 3) == 2)
#define Extract_exception(v) ((v) & ~3)

/* atoms: the zero-sized blocks of each tag */
CAMLextern header_t *caml_atom_table;
#define Atom(tag) (Val_hp(&(caml_atom_table[(tag)])))

/* closures (a stub never reads a closure's code; oi keeps its own) */
#define Code_val(val) (((void **) (val)) [0])

/* the runtime's own parameters (startup_aux.h's caml_params, read by a test's stub) */
struct caml_params {
  uintnat parser_trace;
  uintnat trace_level;
  uintnat runtime_events_log_wsize;
  uintnat verify_heap;
  uintnat print_magic;
  uintnat print_config;
  uintnat init_percent_free;
  uintnat init_minor_heap_wsz;
  uintnat init_custom_major_ratio;
  uintnat init_custom_minor_ratio;
  uintnat init_custom_minor_max_bsz;
  uintnat init_max_stack_wsz;
  uintnat backtrace_enabled;
  uintnat runtime_warnings;
  uintnat cleanup_on_exit;
  uintnat event_trace;
  uintnat max_domains;
};
CAMLextern const struct caml_params *const caml_params;

#ifdef __cplusplus
}
#endif

#endif
