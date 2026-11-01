// 6.10.4: #embed and __has_embed: a resource's bytes as an initializer's elements.
#if __has_embed("pp_embed.dat") == __STDC_EMBED_FOUND__
static const unsigned char data[] = {
#embed "pp_embed.dat"
};
#endif
static const unsigned char limited[] = {
#embed "pp_embed.dat" limit(2) suffix(, 0)
};
int size(void) { return (int)(sizeof data + sizeof limited); }
