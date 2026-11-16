/* oi's C API: Arbace's own clean-room <caml/blake2.h> (test/ocaml/capi/include/caml/mlvalues.h's notes). */
#ifndef CAML_BLAKE2_H
#define CAML_BLAKE2_H
#include "mlvalues.h"
/* the runtime's BLAKE2b (RFC 7693), as runtime.c implements it */
#define BLAKE2_BLOCKSIZE 128
struct BLAKE2_context {
  uint64_t h[8];
  uint64_t len[2];
  size_t numbytes;
  unsigned char buffer[BLAKE2_BLOCKSIZE];
};
CAMLextern void caml_BLAKE2Init(struct BLAKE2_context *s, size_t hashlen, size_t keylen, const unsigned char *key);
CAMLextern void caml_BLAKE2Update(struct BLAKE2_context *s, const unsigned char *data, size_t len);
CAMLextern void caml_BLAKE2Final(struct BLAKE2_context *s, size_t hashlen, unsigned char *hash);
#endif
