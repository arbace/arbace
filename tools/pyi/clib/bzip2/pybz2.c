/* The C side of pyi's _bz2 (queue 208; doc/python/PYTHON-SURVEY.md, "As built: 208"): built by ci's builder with
   bzip2 1.0.8's library (blocksort.c huffman.c crctable.c randtable.c compress.c decompress.c bzlib.c, with
   BZ_NO_STDIO) into one node program, go/arbace/clib/bzip2's Nodes. The Go glue (go/arbace/pybz2) calls the
   library's six stream functions directly, and these helpers: the layout of bz_stream it checks at its start, and
   the internal error handler BZ_NO_STDIO asks the program for. */

#include <stddef.h>
#include <unistd.h>
#include "bzlib.h"

/* bz_stream's size (k = 0) and its fields' offsets (k = 1..9, in declaration order), so the glue's constants are
   the builder's layout and not assumed. */
long pybz2_layout(int k)
{
    switch (k) {
    case 0: return (long)sizeof(bz_stream);
    case 1: return (long)offsetof(bz_stream, next_in);
    case 2: return (long)offsetof(bz_stream, avail_in);
    case 3: return (long)offsetof(bz_stream, total_in_lo32);
    case 4: return (long)offsetof(bz_stream, total_in_hi32);
    case 5: return (long)offsetof(bz_stream, next_out);
    case 6: return (long)offsetof(bz_stream, avail_out);
    case 7: return (long)offsetof(bz_stream, total_out_lo32);
    case 8: return (long)offsetof(bz_stream, total_out_hi32);
    case 9: return (long)offsetof(bz_stream, state);
    }
    return -1;
}

/* BZ_NO_STDIO's handler of a failed internal assertion: the first line of the stdio build's
   BZ2_bz__AssertH__fail on the standard error, then the stdio build's exit(3), as _exit. */
void bz_internal_error(int errcode)
{
    char buf[96], num[16];
    const char *head = "\n\nbzip2/libbzip2: internal error number ";
    size_t n = 0, k = 0;
    unsigned v = errcode < 0 ? 0u - (unsigned)errcode : (unsigned)errcode;
    while (head[n]) { buf[n] = head[n]; n++; }
    if (errcode < 0) buf[n++] = '-';
    do { num[k++] = (char)('0' + v % 10); v /= 10; } while (v);
    while (k) buf[n++] = num[--k];
    buf[n++] = '.';
    buf[n++] = '\n';
    (void)write(2, buf, n);
    _exit(3);
}

/* musl's libc initialization (src/env/__libc_start_main.c): arbace/clib calls it once, before the library's
   first call, as __libc_start_main would (the thread pointer, the auxiliary vector). */
void __init_libc(char **envp, char *pn);

/* A program needs an entry; the library's is never run. It names __init_libc, so the link keeps it. */
int main(void)
{
    __init_libc((char **)0, (char *)0);
    return 0;
}
