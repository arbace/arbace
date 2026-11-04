/* the C side of tools/expat/check.sh's Go/C differential (queue 202e): a document's events and positions */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "expat.h"
static XML_Parser P;
static void pos(const char *ev) { printf("%s %lu %lu %ld", ev, (unsigned long)XML_GetCurrentLineNumber(P), (unsigned long)XML_GetCurrentColumnNumber(P), (long)XML_GetCurrentByteIndex(P)); }
static void s(const XML_Char *x) { if (!x) printf(" NULL"); else printf(" [%s]", x); }
static void sl(const XML_Char *x, int n) { printf(" [%.*s]", n, x); }
static void se(void *u, const XML_Char *n, const XML_Char **a) { pos("start"); s(n); for (; *a; a++) s(*a); printf(" spec=%d\n", XML_GetSpecifiedAttributeCount(P)); }
static void ee(void *u, const XML_Char *n) { pos("end"); s(n); printf("\n"); }
static void cd(void *u, const XML_Char *x, int n) { pos("chars"); sl(x, n); printf("\n"); }
static void pi(void *u, const XML_Char *t, const XML_Char *d) { pos("pi"); s(t); s(d); printf("\n"); }
static void cm(void *u, const XML_Char *d) { pos("comment"); s(d); printf("\n"); }
static void scd(void *u) { pos("scdata"); printf("\n"); }
static void ecd(void *u) { pos("ecdata"); printf("\n"); }
static void df(void *u, const XML_Char *x, int n) { pos("default"); sl(x, n); printf("\n"); }
static void sdt(void *u, const XML_Char *n, const XML_Char *sy, const XML_Char *pu, int h) { pos("sdoctype"); s(n); s(sy); s(pu); printf(" %d\n", h); }
static void edt(void *u) { pos("edoctype"); printf("\n"); }
static void model(XML_Content *m) { printf("(%d %d", m->type, m->quant); s(m->name); for (unsigned i = 0; i < m->numchildren; i++) model(&m->children[i]); printf(")"); }
static void eld(void *u, const XML_Char *n, XML_Content *m) { pos("eldecl"); s(n); printf(" "); model(m); printf("\n"); XML_FreeContentModel(P, m); }
static void atd(void *u, const XML_Char *e, const XML_Char *a, const XML_Char *t, const XML_Char *d, int r) { pos("attdecl"); s(e); s(a); s(t); s(d); printf(" %d\n", r); }
static void end(void *u, const XML_Char *n, int pe, const XML_Char *v, int vl, const XML_Char *b, const XML_Char *sy, const XML_Char *pu, const XML_Char *no) { pos("entdecl"); s(n); printf(" %d", pe); if (v) sl(v, vl); else printf(" NULL"); s(b); s(sy); s(pu); s(no); printf("\n"); }
static void xd(void *u, const XML_Char *v, const XML_Char *e, int st) { pos("xmldecl"); s(v); s(e); printf(" %d\n", st); }
static void nd(void *u, const XML_Char *n, const XML_Char *b, const XML_Char *sy, const XML_Char *pu) { pos("notation"); s(n); s(b); s(sy); s(pu); printf("\n"); }
static void snd(void *u, const XML_Char *p, const XML_Char *uri) { pos("sns"); s(p); s(uri); printf("\n"); }
static void end_ns(void *u, const XML_Char *p) { pos("ens"); s(p); printf("\n"); }
static int ns_(void *u) { pos("notstandalone"); printf("\n"); return 1; }
static int ext(XML_Parser p, const XML_Char *c, const XML_Char *b, const XML_Char *sy, const XML_Char *pu) { pos("extref"); s(c); s(b); s(sy); s(pu); printf("\n"); return 1; }
static void sk(void *u, const XML_Char *n, int pe) { pos("skipped"); s(n); printf(" %d\n", pe); }
int main(int argc, char **argv) {
  /* drv FILE NS(0/1) CHUNK DEFAULT(0/1) */
  FILE *f = fopen(argv[1], "rb"); static char buf[1 << 20]; size_t n = fread(buf, 1, sizeof buf, f); fclose(f);
  int ns = atoi(argv[2]), chunk = atoi(argv[3]), dflt = atoi(argv[4]);
  P = ns ? XML_ParserCreateNS(NULL, '!') : XML_ParserCreate(NULL);
  XML_SetHashSalt(P, 1234567);
  XML_SetElementHandler(P, se, ee); XML_SetCharacterDataHandler(P, cd); XML_SetProcessingInstructionHandler(P, pi);
  XML_SetCommentHandler(P, cm); XML_SetCdataSectionHandler(P, scd, ecd);
  if (dflt) XML_SetDefaultHandlerExpand(P, df);
  XML_SetDoctypeDeclHandler(P, sdt, edt); XML_SetElementDeclHandler(P, eld); XML_SetAttlistDeclHandler(P, atd);
  XML_SetEntityDeclHandler(P, end); XML_SetXmlDeclHandler(P, xd); XML_SetNotationDeclHandler(P, nd);
  XML_SetNamespaceDeclHandler(P, snd, end_ns); XML_SetNotStandaloneHandler(P, ns_);
  XML_SetExternalEntityRefHandler(P, ext); XML_SetSkippedEntityHandler(P, sk);
  XML_SetParamEntityParsing(P, XML_PARAM_ENTITY_PARSING_UNLESS_STANDALONE);
  int st = XML_STATUS_OK;
  if (chunk <= 0) st = XML_Parse(P, buf, (int)n, 1);
  else { size_t i; for (i = 0; i < n && st == XML_STATUS_OK; i += chunk) st = XML_Parse(P, buf + i, (int)(n - i < (size_t)chunk ? n - i : chunk), 0); if (st == XML_STATUS_OK) st = XML_Parse(P, buf, 0, 1); }
  if (st == XML_STATUS_ERROR) printf("error %d %lu %lu %ld [%s]\n", XML_GetErrorCode(P), (unsigned long)XML_GetErrorLineNumber(P), (unsigned long)XML_GetErrorColumnNumber(P), (long)XML_GetErrorByteIndex(P), XML_ErrorString(XML_GetErrorCode(P)));
  else printf("ok\n");
  XML_ParserFree(P); return 0;
}
