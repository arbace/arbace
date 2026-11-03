// synth-c23/pp-embed.c, written by bin/ci synth from test/c/synth/table.edn; edit the table, not this file.
// C23's #embed: a resource's bytes, limit, prefix, suffix, if_empty, __has_embed
// ISO/IEC 9899:2024 6.10.4; Annex A: control-line (#embed), embed-parameter-sequence, has-embed-expression
// M.2: #embed and __has_embed
#include <stdio.h>

static const unsigned char all[] = {
#embed "synth-embed.dat"
};
static const unsigned char two[] = {
#embed "synth-embed.dat" limit(2)
};
static const int framed[] = {
#embed "synth-embed.dat" prefix(100, ) suffix(, 200) limit(1)
};
static const int none[] = {
#embed "synth-empty.dat" if_empty(42)
};
static const int count = sizeof((const unsigned char[]){
#embed "synth-embed.dat"
});
#if __has_embed("synth-embed.dat") == __STDC_EMBED_FOUND__ && __has_embed("synth-empty.dat") == __STDC_EMBED_EMPTY__ && __has_embed("no-such-synth.dat") == __STDC_EMBED_NOT_FOUND__
static int has_ok = 1;
#else
static int has_ok = 0;
#endif

int main(void) {
  printf("%s = %zu\n", "sizeof all", sizeof all);
  printf("%s = %d\n", "all[0]", all[0]);
  printf("%s = %d\n", "all[3]", all[3]);
  printf("%s = %d\n", "all[4]", all[4]);
  printf("%s = %zu\n", "sizeof two", sizeof two);
  printf("%s = %d\n", "two[1]", two[1]);
  printf("%s = %zu\n", "sizeof framed / sizeof framed[0]", sizeof framed / sizeof framed[0]);
  printf("%s = %d\n", "framed[0]", framed[0]);
  printf("%s = %d\n", "framed[1]", framed[1]);
  printf("%s = %d\n", "framed[2]", framed[2]);
  printf("%s = %d\n", "none[0]", none[0]);
  printf("%s = %d\n", "count", count);
  printf("%s = %d\n", "has_ok", has_ok);
  printf("%s = %d\n", "__STDC_EMBED_NOT_FOUND__", __STDC_EMBED_NOT_FOUND__);
  printf("%s = %d\n", "__STDC_EMBED_FOUND__", __STDC_EMBED_FOUND__);
  printf("%s = %d\n", "__STDC_EMBED_EMPTY__", __STDC_EMBED_EMPTY__);
  return 0;
}
