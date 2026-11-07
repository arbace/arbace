/* The C side of pyi's _zstd (queue 208; doc/python/PYTHON-SURVEY.md, "After the decisions"): built by ci's builder
   with zstd 1.5.7's library (lib/common, lib/compress, lib/decompress, lib/dictBuilder; no legacy formats, no
   assembly, no intrinsics, no threads) into one node program, go/arbace/clib/zstd's Nodes. The Go glue
   (go/arbace/pyzstd) calls zstd's functions directly, and these helpers: the layouts it checks at its start, and
   wrappers for the calls that take or return a structure by value (ZSTD_bounds, ZDICT_params_t). */

#include <stddef.h>
#include <string.h>
#define ZSTD_STATIC_LINKING_ONLY
#define ZDICT_STATIC_LINKING_ONLY
#include "zstd.h"
#include "zdict.h"

/* ZSTD_inBuffer's size (k = 0) and the offsets of src, size, pos (1..3); ZSTD_outBuffer's size (4) and the
   offsets of dst, size, pos (5..7). */
long pyzstd_layout(int k)
{
    switch (k) {
    case 0: return (long)sizeof(ZSTD_inBuffer);
    case 1: return (long)offsetof(ZSTD_inBuffer, src);
    case 2: return (long)offsetof(ZSTD_inBuffer, size);
    case 3: return (long)offsetof(ZSTD_inBuffer, pos);
    case 4: return (long)sizeof(ZSTD_outBuffer);
    case 5: return (long)offsetof(ZSTD_outBuffer, dst);
    case 6: return (long)offsetof(ZSTD_outBuffer, size);
    case 7: return (long)offsetof(ZSTD_outBuffer, pos);
    }
    return -1;
}

/* ZSTD_cParam_getBounds / ZSTD_dParam_getBounds (decompress != 0): which 0 its error, 1 lowerBound, 2 upperBound. */
long pyzstd_bounds(int decompress, int param, int which)
{
    ZSTD_bounds b = decompress ? ZSTD_dParam_getBounds((ZSTD_dParameter)param)
                               : ZSTD_cParam_getBounds((ZSTD_cParameter)param);
    switch (which) {
    case 0: return (long)b.error;
    case 1: return (long)b.lowerBound;
    case 2: return (long)b.upperBound;
    }
    return 0;
}

/* ZDICT_finalizeDictionary with _zstdmodule.c's params (compressionLevel, notificationLevel 0, dictID). */
size_t pyzstd_finalize(void *dst, size_t dst_cap, const void *content, size_t content_size, const void *samples,
                       const size_t *sizes, unsigned n, int level, unsigned dict_id)
{
    ZDICT_params_t params;
    memset(&params, 0, sizeof params);
    params.compressionLevel = level;
    params.notificationLevel = 0;
    params.dictID = dict_id;
    return ZDICT_finalizeDictionary(dst, dst_cap, content, content_size, samples, sizes, n, params);
}

/* musl's libc initialization, which arbace/clib calls once (see bzip2's pybz2.c). */
void __init_libc(char **envp, char *pn);

/* A program needs an entry; the library's is never run. It names __init_libc, so the link keeps it. */
int main(void)
{
    __init_libc((char **)0, (char *)0);
    return 0;
}
