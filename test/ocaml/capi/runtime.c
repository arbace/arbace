/* oi's C API on ci (doc/OCAML-SURVEY.md, "As built: the C API on ci"): the C half of the runtime a test's C stubs link
   with, clean-room from OCaml's manual (chapter 22, "Interfacing C with OCaml") over Arbace's own headers
   (test/ocaml/capi/include/caml). The functions declared and not defined here (__oi_alloc, __oi_alloc_static,
   caml_callbackN_exn, caml_raise, caml_named_value, caml_output_value_to_block, caml_input_value_from_block) are
   natives of ci's program that oi's side (go/arbace/oicapi) gives: they cross into oi's values. A block a stub allocates is
   in the boundary's own memory, a copy oi reads when the stub returns; a custom block's is the static memory's, kept for
   the run. bin/oi links this file with the stubs and musl (test/ocaml/capi/cnodes.ae). */
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include "caml/mlvalues.h"
#include "caml/memory.h"
#include "caml/alloc.h"
#include "caml/callback.h"
#include "caml/fail.h"
#include "caml/custom.h"
#include "caml/bigarray.h"
#include "caml/signals.h"
#include "caml/threads.h"
#include "caml/intext.h"
#include "caml/blake2.h"
#include "caml/misc.h"
#include "caml/domain.h"
#include "caml/domain_state.h"

/* the natives (go/arbace/oicapi) */
extern value __oi_alloc(mlsize_t wosize, tag_t tag);
extern value __oi_alloc_static(mlsize_t wosize, tag_t tag);
extern value __oi_predef(int index);

/* the atoms: zero-sized blocks, one a tag */
static header_t atoms[256];
header_t *caml_atom_table = atoms;

static struct caml_params params = { .init_max_stack_wsz = 128 * 1024 * 1024, .max_domains = 128 };
const struct caml_params *const caml_params = &params;

/* the custom operations of the runtime's own custom blocks: the identifiers oi reads */
static const struct custom_operations int32_ops = { "_i" };
static const struct custom_operations int64_ops = { "_j" };
static const struct custom_operations nativeint_ops = { "_n" };
static const struct custom_operations bigarray_ops = { "_bigarr02" };
const struct custom_operations *__oi_ops[4] = { &int32_ops, &int64_ops, &nativeint_ops, &bigarray_ops };

void __oi_c_init(void)
{
  for (int i = 0; i < 256; i++) atoms[i] = Make_header(0, i, 0);
}

void __oi_c_flush(void) { fflush(0); }

/* allocation */
value caml_alloc(mlsize_t wosize, tag_t tag)
{
  value v = __oi_alloc(wosize, tag);
  if (tag < No_scan_tag) for (mlsize_t i = 0; i < wosize; i++) Field(v, i) = Val_unit;
  return v;
}
value caml_alloc_small(mlsize_t wosize, tag_t tag) { return caml_alloc(wosize, tag); }
value caml_alloc_shr(mlsize_t wosize, tag_t tag) { return caml_alloc(wosize, tag); }
value caml_alloc_tuple(mlsize_t n) { return caml_alloc(n, 0); }
value caml_alloc_record(tag_t tag, mlsize_t n) { return caml_alloc(n, tag); }
value caml_alloc_float_array(mlsize_t n) { return n == 0 ? Atom(0) : __oi_alloc(n, Double_array_tag); }
value caml_alloc_some(value v) { value r = caml_alloc(1, 0); Field(r, 0) = v; return r; }

value caml_alloc_string(mlsize_t len)
{
  mlsize_t wosize = (len + sizeof(value)) / sizeof(value);
  value v = __oi_alloc(wosize, String_tag);
  Field(v, wosize - 1) = 0;
  mlsize_t last = Bsize_wsize(wosize) - 1;
  Byte(v, last) = (char) (last - len);
  return v;
}
mlsize_t caml_string_length(value s)
{
  mlsize_t last = Bosize_val(s) - 1;
  return last - Byte_u(s, last);
}
int caml_string_is_c_safe(value s) { return strlen(String_val(s)) == caml_string_length(s); }
value caml_alloc_initialized_string(mlsize_t len, const char *p)
{
  value v = caml_alloc_string(len);
  memcpy(Bytes_val(v), p, len);
  return v;
}
value caml_copy_string(const char *s) { return caml_alloc_initialized_string(strlen(s), s); }
value caml_alloc_array(value (*funct)(const char *), const char **arr)
{
  mlsize_t n = 0;
  while (arr[n] != NULL) n++;
  if (n == 0) return Atom(0);
  value r = caml_alloc(n, 0);
  for (mlsize_t i = 0; i < n; i++) Field(r, i) = funct(arr[i]);
  return r;
}
value caml_copy_string_array(const char **arr) { return caml_alloc_array(caml_copy_string, arr); }
value caml_alloc_sprintf(const char *format, ...)
{
  va_list a, b;
  va_start(a, format);
  va_copy(b, a);
  int n = vsnprintf(NULL, 0, format, a);
  va_end(a);
  value v = caml_alloc_string(n);
  char *buf = malloc(n + 1);
  vsnprintf(buf, n + 1, format, b);
  va_end(b);
  memcpy(Bytes_val(v), buf, n);
  free(buf);
  return v;
}
value caml_copy_double(double d) { value v = __oi_alloc(Double_wosize, Double_tag); Store_double_val(v, d); return v; }

value caml_alloc_custom(const struct custom_operations *ops, uintnat size, mlsize_t mem, mlsize_t max)
{
  value v = __oi_alloc_static(1 + (size + sizeof(value) - 1) / sizeof(value), Custom_tag);
  Custom_ops_val(v) = ops;
  return v;
}
value caml_alloc_custom_mem(const struct custom_operations *ops, uintnat size, mlsize_t mem)
{ return caml_alloc_custom(ops, size, mem, 0); }
value caml_alloc_custom_dep(const struct custom_operations *ops, uintnat size, mlsize_t mem)
{ return caml_alloc_custom(ops, size, mem, 0); }

value caml_alloc_final(mlsize_t len, final_fun f, mlsize_t mem, mlsize_t max)
{
  value v = __oi_alloc_static(len, Custom_tag);
  (void) f;
  return v;
}

static value boxed(int i, mlsize_t words)
{
  value v = __oi_alloc(1 + words, Custom_tag);
  Custom_ops_val(v) = __oi_ops[i];
  return v;
}
value caml_copy_int32(int32_t i) { value v = boxed(0, 1); Field(v, 1) = 0; Int32_val(v) = i; return v; }
value caml_copy_int64(int64_t i) { value v = boxed(1, 1); Int64_val(v) = i; return v; }
value caml_copy_nativeint(intnat i) { value v = boxed(2, 1); Nativeint_val(v) = i; return v; }

mlsize_t caml_array_length(value v)
{
  return Tag_val(v) == Double_array_tag ? Wosize_val(v) / Double_wosize : Wosize_val(v);
}
int caml_is_double_array(value v) { return Tag_val(v) == Double_array_tag; }
int caml_convert_flag_list(value list, const int *flags)
{
  int res = 0;
  for (; list != Val_emptylist; list = Field(list, 1)) res |= flags[Int_val(Field(list, 0))];
  return res;
}

/* fields, roots */
void caml_modify(volatile value *fp, value v) { *fp = v; }
void caml_initialize(volatile value *fp, value v) { *fp = v; }
int caml_atomic_cas_field(value obj, intnat i, value old, value nv)
{
  if (Field(obj, i) == old) { Field(obj, i) = nv; return 1; }
  return 0;
}
void caml_register_global_root(value *r) { (void) r; }
void caml_remove_global_root(value *r) { (void) r; }
void caml_register_generational_global_root(value *r) { (void) r; }
void caml_remove_generational_global_root(value *r) { (void) r; }
void caml_modify_generational_global_root(value *r, value v) { *r = v; }
void caml_alloc_dependent_memory(value v, mlsize_t n) { (void) v; (void) n; }
void caml_free_dependent_memory(value v, mlsize_t n) { (void) v; (void) n; }
void caml_adjust_gc_speed(mlsize_t a, mlsize_t b) { (void) a; (void) b; }
void *caml_stat_alloc(size_t n)
{
  void *p = malloc(n);
  if (p == NULL && n != 0) caml_raise_out_of_memory();
  return p;
}
void *caml_stat_alloc_noexc(size_t n) { return malloc(n); }
void *caml_stat_calloc_noexc(size_t n, size_t s) { return calloc(n, s); }
void *caml_stat_resize(void *p, size_t n) { return realloc(p, n); }
void caml_stat_free(void *p) { free(p); }
char *caml_stat_strdup(const char *s) { return strdup(s); }

/* callbacks */
value caml_callbackN(value closure, int narg, value args[])
{
  value r = caml_callbackN_exn(closure, narg, args);
  if (Is_exception_result(r)) caml_raise(Extract_exception(r));
  return r;
}
value caml_callback_exn(value f, value a) { value args[1] = { a }; return caml_callbackN_exn(f, 1, args); }
value caml_callback2_exn(value f, value a, value b) { value args[2] = { a, b }; return caml_callbackN_exn(f, 2, args); }
value caml_callback3_exn(value f, value a, value b, value c)
{ value args[3] = { a, b, c }; return caml_callbackN_exn(f, 3, args); }
value caml_callback(value f, value a) { value args[1] = { a }; return caml_callbackN(f, 1, args); }
value caml_callback2(value f, value a, value b) { value args[2] = { a, b }; return caml_callbackN(f, 2, args); }
value caml_callback3(value f, value a, value b, value c) { value args[3] = { a, b, c }; return caml_callbackN(f, 3, args); }

/* exceptions: the predefined ones by fail.h's indexes, from oi */
void caml_raise_constant(value tag) { caml_raise(tag); }
void caml_raise_with_arg(value tag, value arg)
{
  value b = caml_alloc(2, 0);
  Field(b, 0) = tag;
  Field(b, 1) = arg;
  caml_raise(b);
}
void caml_raise_with_args(value tag, int nargs, value args[])
{
  value b = caml_alloc(1 + nargs, 0);
  Field(b, 0) = tag;
  for (int i = 0; i < nargs; i++) Field(b, 1 + i) = args[i];
  caml_raise(b);
}
void caml_raise_with_string(value tag, const char *msg) { caml_raise_with_arg(tag, caml_copy_string(msg)); }
void caml_failwith(const char *msg) { caml_raise_with_string(__oi_predef(2), msg); }
void caml_failwith_value(value msg) { caml_raise_with_arg(__oi_predef(2), msg); }
void caml_invalid_argument(const char *msg) { caml_raise_with_string(__oi_predef(3), msg); }
void caml_invalid_argument_value(value msg) { caml_raise_with_arg(__oi_predef(3), msg); }
void caml_raise_out_of_memory(void) { caml_raise_constant(__oi_predef(0)); }
void caml_raise_sys_error(value msg) { caml_raise_with_arg(__oi_predef(1), msg); }
void caml_raise_end_of_file(void) { caml_raise_constant(__oi_predef(4)); }
void caml_raise_zero_divide(void) { caml_raise_constant(__oi_predef(5)); }
void caml_raise_not_found(void) { caml_raise_constant(__oi_predef(6)); }
void caml_raise_stack_overflow(void) { caml_raise_constant(__oi_predef(8)); }
void caml_raise_sys_blocked_io(void) { caml_raise_constant(__oi_predef(9)); }
void caml_array_bound_error(void) { caml_invalid_argument("index out of bounds"); }
void caml_fatal_error(const char *msg, ...)
{
  va_list a;
  va_start(a, msg);
  fprintf(stderr, "Fatal error: ");
  vfprintf(stderr, msg, a);
  fprintf(stderr, "\n");
  va_end(a);
  abort();
}

/* the runtime lock: oi's threads run one at a time already, and a stub's blocking section releases nothing; it only marks the
   thread outside the runtime, where Caml_state_opt is NULL (signals.c's caml_enter_blocking_section clears the domain's state) */
static int blocking;
struct caml_domain_state { int unused; };
static struct caml_domain_state domain_state;
caml_domain_state *caml_state_opt(void) { return blocking ? NULL : &domain_state; }
void caml_enter_blocking_section(void) { blocking = 1; }
void caml_enter_blocking_section_no_pending(void) { blocking = 1; }
void caml_leave_blocking_section(void) { blocking = 0; }
void caml_process_pending_actions(void) {}
value caml_process_pending_actions_exn(void) { return Val_unit; }
int caml_check_pending_actions(void) { return 0; }
int caml_c_thread_register(void) { return 1; }
int caml_c_thread_unregister(void) { return 1; }
int caml_domain_alone(void) { return 1; }

/* signal numbers: the Sys.sigXXX numbers (negative) against the system's (runtime/signals.c's table, its order) */
static const int posix_signals[] = {
  SIGABRT, SIGALRM, SIGFPE, SIGHUP, SIGILL, SIGINT, SIGKILL, SIGPIPE, SIGQUIT, SIGSEGV, SIGTERM, SIGUSR1, SIGUSR2,
  SIGCHLD, SIGCONT, SIGSTOP, SIGTSTP, SIGTTIN, SIGTTOU, SIGVTALRM, SIGPROF, SIGBUS, SIGPOLL, SIGSYS, SIGTRAP, SIGURG,
  SIGXCPU, SIGXFSZ
};
int caml_convert_signal_number(int s)
{
  if (s < 0 && s >= -(int) (sizeof(posix_signals) / sizeof(int))) return posix_signals[-s - 1];
  return s;
}
int caml_rev_convert_signal_number(int s)
{
  for (int i = 0; i < (int) (sizeof(posix_signals) / sizeof(int)); i++)
    if (s == posix_signals[i]) return -i - 1;
  return s;
}

/* bigarrays: the custom block holds struct caml_ba_array after the operations' pointer */
int caml_ba_element_size[] = { 4, 8, 1, 1, 2, 2, 4, 8, sizeof(value), sizeof(value), 8, 16, 1, 2 };
uintnat caml_ba_num_elts(struct caml_ba_array *b)
{
  uintnat n = 1;
  for (int i = 0; i < b->num_dims; i++) n *= b->dim[i];
  return n;
}
uintnat caml_ba_byte_size(struct caml_ba_array *b)
{
  return caml_ba_num_elts(b) * caml_ba_element_size[b->flags & CAML_BA_KIND_MASK];
}
value caml_ba_alloc(int flags, int num_dims, void *data, intnat *dim)
{
  if (num_dims < 0 || num_dims > CAML_BA_MAX_NUM_DIMS) caml_invalid_argument("Bigarray.create: bad number of dimensions");
  uintnat n = 1;
  for (int i = 0; i < num_dims; i++) {
    if (dim[i] < 0) caml_invalid_argument("Bigarray.create: negative dimension");
    n *= dim[i];
  }
  if (data == NULL) {
    data = calloc(n == 0 ? 1 : n, caml_ba_element_size[flags & CAML_BA_KIND_MASK]);
    if (data == NULL) caml_raise_out_of_memory();
    flags |= CAML_BA_MANAGED;
  }
  value v = __oi_alloc(1 + (sizeof(struct caml_ba_array) + num_dims * sizeof(intnat)) / sizeof(value), Custom_tag);
  Custom_ops_val(v) = &bigarray_ops;
  struct caml_ba_array *b = Caml_ba_array_val(v);
  b->data = data;
  b->num_dims = num_dims;
  b->flags = flags;
  b->proxy = NULL;
  for (int i = 0; i < num_dims; i++) b->dim[i] = dim[i];
  return v;
}
value caml_ba_alloc_dims(int flags, int num_dims, void *data, ...)
{
  intnat dim[CAML_BA_MAX_NUM_DIMS];
  va_list a;
  va_start(a, data);
  for (int i = 0; i < num_dims; i++) dim[i] = va_arg(a, intnat);
  va_end(a);
  return caml_ba_alloc(flags, num_dims, data, dim);
}

/* BLAKE2b, written from RFC 7693 (section 3 and appendix C's description) */
static const uint64_t blake2_iv[8] = {
  0x6A09E667F3BCC908ULL, 0xBB67AE8584CAA73BULL, 0x3C6EF372FE94F82BULL, 0xA54FF53A5F1D36F1ULL,
  0x510E527FADE682D1ULL, 0x9B05688C2B3E6C1FULL, 0x1F83D9ABFB41BD6BULL, 0x5BE0CD19137E2179ULL
};
static const unsigned char blake2_sigma[12][16] = {
  { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, { 14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3 },
  { 11, 8, 12, 0, 5, 2, 15, 13, 10, 14, 3, 6, 7, 1, 9, 4 }, { 7, 9, 3, 1, 13, 12, 11, 14, 2, 6, 5, 10, 4, 0, 15, 8 },
  { 9, 0, 5, 7, 2, 4, 10, 15, 14, 1, 11, 12, 6, 8, 3, 13 }, { 2, 12, 6, 10, 0, 11, 8, 3, 4, 13, 7, 5, 15, 14, 1, 9 },
  { 12, 5, 1, 15, 14, 13, 4, 10, 0, 7, 6, 3, 9, 2, 8, 11 }, { 13, 11, 7, 14, 12, 1, 3, 9, 5, 0, 15, 4, 8, 6, 2, 10 },
  { 6, 15, 14, 9, 11, 3, 0, 8, 12, 2, 13, 7, 1, 4, 10, 5 }, { 10, 2, 8, 4, 7, 6, 1, 5, 15, 11, 9, 14, 3, 12, 13, 0 },
  { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, { 14, 10, 4, 8, 9, 15, 13, 6, 1, 12, 0, 2, 11, 7, 5, 3 }
};
static uint64_t rotr64(uint64_t x, int n) { return (x >> n) | (x << (64 - n)); }
static void blake2_mix(uint64_t *v, int a, int b, int c, int d, uint64_t x, uint64_t y)
{
  v[a] = v[a] + v[b] + x; v[d] = rotr64(v[d] ^ v[a], 32);
  v[c] = v[c] + v[d];     v[b] = rotr64(v[b] ^ v[c], 24);
  v[a] = v[a] + v[b] + y; v[d] = rotr64(v[d] ^ v[a], 16);
  v[c] = v[c] + v[d];     v[b] = rotr64(v[b] ^ v[c], 63);
}
static void blake2_compress(struct BLAKE2_context *s, const unsigned char *data, int last)
{
  uint64_t v[16], m[16];
  for (int i = 0; i < 8; i++) { v[i] = s->h[i]; v[i + 8] = blake2_iv[i]; }
  v[12] ^= s->len[0];
  v[13] ^= s->len[1];
  if (last) v[14] = ~v[14];
  for (int i = 0; i < 16; i++) {
    uint64_t w = 0;
    for (int j = 7; j >= 0; j--) w = (w << 8) | data[8 * i + j];
    m[i] = w;
  }
  for (int r = 0; r < 12; r++) {
    const unsigned char *z = blake2_sigma[r];
    blake2_mix(v, 0, 4, 8, 12, m[z[0]], m[z[1]]);
    blake2_mix(v, 1, 5, 9, 13, m[z[2]], m[z[3]]);
    blake2_mix(v, 2, 6, 10, 14, m[z[4]], m[z[5]]);
    blake2_mix(v, 3, 7, 11, 15, m[z[6]], m[z[7]]);
    blake2_mix(v, 0, 5, 10, 15, m[z[8]], m[z[9]]);
    blake2_mix(v, 1, 6, 11, 12, m[z[10]], m[z[11]]);
    blake2_mix(v, 2, 7, 8, 13, m[z[12]], m[z[13]]);
    blake2_mix(v, 3, 4, 9, 14, m[z[14]], m[z[15]]);
  }
  for (int i = 0; i < 8; i++) s->h[i] ^= v[i] ^ v[i + 8];
}
void caml_BLAKE2Init(struct BLAKE2_context *s, size_t hashlen, size_t keylen, const unsigned char *key)
{
  for (int i = 0; i < 8; i++) s->h[i] = blake2_iv[i];
  s->h[0] ^= 0x01010000ULL ^ ((uint64_t) keylen << 8) ^ hashlen;
  s->len[0] = s->len[1] = 0;
  s->numbytes = 0;
  if (keylen > 0) {
    memset(s->buffer, 0, BLAKE2_BLOCKSIZE);
    memcpy(s->buffer, key, keylen);
    s->numbytes = BLAKE2_BLOCKSIZE;
  }
}
void caml_BLAKE2Update(struct BLAKE2_context *s, const unsigned char *data, size_t len)
{
  for (size_t i = 0; i < len; i++) {
    if (s->numbytes == BLAKE2_BLOCKSIZE) {
      s->len[0] += BLAKE2_BLOCKSIZE;
      if (s->len[0] < BLAKE2_BLOCKSIZE) s->len[1]++;
      blake2_compress(s, s->buffer, 0);
      s->numbytes = 0;
    }
    s->buffer[s->numbytes++] = data[i];
  }
}
void caml_BLAKE2Final(struct BLAKE2_context *s, size_t hashlen, unsigned char *hash)
{
  s->len[0] += s->numbytes;
  if (s->len[0] < s->numbytes) s->len[1]++;
  memset(s->buffer + s->numbytes, 0, BLAKE2_BLOCKSIZE - s->numbytes);
  blake2_compress(s, s->buffer, 1);
  for (size_t i = 0; i < hashlen; i++) hash[i] = (unsigned char) (s->h[i / 8] >> (8 * (i % 8)));
}

/* custom blocks' serialization (intext.h): the operations' serialize writes into a buffer of the boundary's, deserialize reads
   one; oi's Marshal calls them through go/arbace/oicapi (__oi_custom_serialize, __oi_custom_deserialize) */
static unsigned char *ser_buf;
static uintnat ser_len, ser_cap;
static const unsigned char *de_buf;
static uintnat de_pos;
static void ser_put(const void *p, uintnat n)
{
  if (ser_len + n > ser_cap) {
    ser_cap = (ser_len + n) * 2 + 64;
    ser_buf = realloc(ser_buf, ser_cap);
    if (ser_buf == NULL) caml_raise_out_of_memory();
  }
  memcpy(ser_buf + ser_len, p, n);
  ser_len += n;
}
static void ser_be(uint64_t x, int n)
{
  unsigned char b[8];
  for (int i = 0; i < n; i++) b[i] = (unsigned char) (x >> (8 * (n - 1 - i)));
  ser_put(b, n);
}
void caml_serialize_int_1(int i) { ser_be((uint64_t) i, 1); }
void caml_serialize_int_2(int i) { ser_be((uint64_t) i, 2); }
void caml_serialize_int_4(int32_t i) { ser_be((uint64_t) (uint32_t) i, 4); }
void caml_serialize_int_8(int64_t i) { ser_be((uint64_t) i, 8); }
void caml_serialize_float_4(float f) { uint32_t u; memcpy(&u, &f, 4); ser_be(u, 4); }
void caml_serialize_float_8(double f) { uint64_t u; memcpy(&u, &f, 8); ser_be(u, 8); }
void caml_serialize_block_1(void *data, intnat len) { ser_put(data, len); }
static uint64_t de_be(int n)
{
  uint64_t x = 0;
  for (int i = 0; i < n; i++) x = (x << 8) | de_buf[de_pos++];
  return x;
}
int caml_deserialize_uint_1(void) { return (int) de_be(1); }
int caml_deserialize_sint_1(void) { return (int) (int8_t) de_be(1); }
int caml_deserialize_uint_2(void) { return (int) de_be(2); }
int caml_deserialize_sint_2(void) { return (int) (int16_t) de_be(2); }
uint32_t caml_deserialize_uint_4(void) { return (uint32_t) de_be(4); }
int32_t caml_deserialize_sint_4(void) { return (int32_t) de_be(4); }
uint64_t caml_deserialize_uint_8(void) { return de_be(8); }
int64_t caml_deserialize_sint_8(void) { return (int64_t) de_be(8); }
void caml_deserialize_block_1(void *data, intnat len) { memcpy(data, de_buf + de_pos, len); de_pos += len; }
void caml_deserialize_error(char *msg) { caml_failwith(msg); }

static const struct custom_operations *registered[64];
static int nregistered;
void caml_register_custom_operations(const struct custom_operations *ops)
{
  if (nregistered < 64) registered[nregistered++] = ops;
}
intnat __oi_custom_serialize(value v, uintnat *sizes)
{
  const struct custom_operations *ops = Custom_ops_val(v);
  if (ops == NULL || ops->serialize == NULL) return -1;
  ser_len = 0;
  sizes[0] = sizes[1] = 0;
  ops->serialize(v, &sizes[0], &sizes[1]);
  return (intnat) ser_len;
}
unsigned char *__oi_custom_buffer(void) { return ser_buf; }
value __oi_custom_deserialize(const char *id, const unsigned char *data, uintnat expected, uintnat *res)
{
  for (int i = 0; i < nregistered; i++) {
    const struct custom_operations *ops = registered[i];
    if (strcmp(ops->identifier, id) != 0 || ops->deserialize == NULL) continue;
    value v = caml_alloc_custom(ops, expected, 0, 1);
    de_buf = data;
    de_pos = 0;
    res[0] = ops->deserialize(Data_custom_val(v));
    res[1] = de_pos;
    return v;
  }
  return 0;
}
