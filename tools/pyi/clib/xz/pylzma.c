/* The C side of pyi's _lzma (queue 208; doc/python/PYTHON-SURVEY.md, "As built: 208"): built by ci's builder with
   XZ Utils 5.2.5's liblzma (src/liblzma's liblzma.la, with this directory's config.h) into one node program,
   go/arbace/clib/xz's Nodes. The Go glue (go/arbace/pylzma) calls liblzma's functions directly (lzma_code, the
   encoders' and decoders' initializers, lzma_end, lzma_get_check, lzma_check_is_supported, the properties' size,
   encode and decode), and these helpers: the layouts it checks at its start, and the option structures and filter
   chains of _lzmamodule.c's filter specifiers, made and read field by field (their layout stays liblzma's). */

#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "lzma.h"

/* lzma_stream's size (k = 0), the offsets of next_in, avail_in, next_out, avail_out (k = 1..4), lzma_filter's size
   (k = 5), LZMA_FILTERS_MAX (k = 6). */
long pylzma_layout(int k)
{
    switch (k) {
    case 0: return (long)sizeof(lzma_stream);
    case 1: return (long)offsetof(lzma_stream, next_in);
    case 2: return (long)offsetof(lzma_stream, avail_in);
    case 3: return (long)offsetof(lzma_stream, next_out);
    case 4: return (long)offsetof(lzma_stream, avail_out);
    case 5: return (long)sizeof(lzma_filter);
    case 6: return (long)LZMA_FILTERS_MAX;
    }
    return -1;
}

/* A filter chain of LZMA_FILTERS_MAX + 1 entries, every id LZMA_VLI_UNKNOWN (parse_filter_chain_spec's array). */
lzma_filter *pylzma_chain(void)
{
    lzma_filter *f = calloc(LZMA_FILTERS_MAX + 1, sizeof *f);
    if (f != NULL)
        for (int i = 0; i <= LZMA_FILTERS_MAX; i++)
            f[i].id = LZMA_VLI_UNKNOWN;
    return f;
}

void pylzma_chain_set(lzma_filter *f, int i, uint64_t id, void *options)
{
    f[i].id = id;
    f[i].options = options;
}

uint64_t pylzma_chain_id(const lzma_filter *f, int i) { return f[i].id; }
void *pylzma_chain_options(const lzma_filter *f, int i) { return f[i].options; }

/* free_filter_chain, then the array: the options are malloc's (the glue's or lzma_properties_decode's, with the
   default allocator). */
void pylzma_chain_free(lzma_filter *f)
{
    if (f == NULL)
        return;
    for (int i = 0; f[i].id != LZMA_VLI_UNKNOWN; i++)
        free(f[i].options);
    free(f);
}

/* parse_filter_spec_lzma's options: zeroed, filled by lzma_lzma_preset (nonzero: an invalid preset), then field
   by field (k: 0 dict_size, 1 lc, 2 lp, 3 pb, 4 mode, 5 nice_len, 6 mf, 7 depth). */
lzma_options_lzma *pylzma_lzma_new(void) { return calloc(1, sizeof(lzma_options_lzma)); }
int pylzma_lzma_preset(lzma_options_lzma *o, uint32_t preset) { return lzma_lzma_preset(o, preset) ? 1 : 0; }

void pylzma_lzma_set(lzma_options_lzma *o, int k, uint32_t v)
{
    switch (k) {
    case 0: o->dict_size = v; break;
    case 1: o->lc = v; break;
    case 2: o->lp = v; break;
    case 3: o->pb = v; break;
    case 4: o->mode = (lzma_mode)v; break;
    case 5: o->nice_len = v; break;
    case 6: o->mf = (lzma_match_finder)v; break;
    case 7: o->depth = v; break;
    }
}

uint32_t pylzma_lzma_get(const lzma_options_lzma *o, int k)
{
    switch (k) {
    case 0: return o->dict_size;
    case 1: return o->lc;
    case 2: return o->lp;
    case 3: return o->pb;
    case 4: return (uint32_t)o->mode;
    case 5: return o->nice_len;
    case 6: return (uint32_t)o->mf;
    case 7: return o->depth;
    }
    return 0;
}

/* parse_filter_spec_delta's and parse_filter_spec_bcj's options, and their one field. */
lzma_options_delta *pylzma_delta_new(uint32_t dist)
{
    lzma_options_delta *o = calloc(1, sizeof *o);
    if (o != NULL) {
        o->type = LZMA_DELTA_TYPE_BYTE;
        o->dist = dist;
    }
    return o;
}

uint32_t pylzma_delta_dist(const lzma_options_delta *o) { return o->dist; }

lzma_options_bcj *pylzma_bcj_new(uint32_t start_offset)
{
    lzma_options_bcj *o = calloc(1, sizeof *o);
    if (o != NULL)
        o->start_offset = start_offset;
    return o;
}

uint32_t pylzma_bcj_start_offset(const lzma_options_bcj *o) { return o->start_offset; }

/* Compressor_init_alone's preset branch: -1 for an invalid preset, else lzma_alone_encoder's return. */
int pylzma_alone_preset(lzma_stream *lzs, uint32_t preset)
{
    lzma_options_lzma options;
    if (lzma_lzma_preset(&options, preset))
        return -1;
    return (int)lzma_alone_encoder(lzs, &options);
}

/* musl's libc initialization, which arbace/clib calls once (see bzip2's pybz2.c). */
void __init_libc(char **envp, char *pn);

/* A program needs an entry; the library's is never run. It names __init_libc, so the link keeps it. */
int main(void)
{
    __init_libc((char **)0, (char *)0);
    return 0;
}
