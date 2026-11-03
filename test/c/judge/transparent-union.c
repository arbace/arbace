/* { dg-do compile } */
/* { dg-options "-std=gnu23" } */
/* Queue 158: a redeclaration through a union parameter (c-typeck.cc's type_lists_compatible_p): a transparent union, or a
   union with no name, of the other parameter's size with a member of its type. Reduced from torture-compile's pr34885.c
   (a transparent typedef) and pr46866.c (an anonymous union, transparent). */
struct sockaddr;
typedef union {
  const struct sockaddr *__restrict sa;
} A __attribute__ ((__transparent_union__));
extern int sendto (A addr);
int sendto (const struct sockaddr *to) { return 0; }
void destroy (union { void *this; } __attribute__ ((transparent_union)));
void destroy (void *this);
union U { int *p; long *l; } __attribute__ ((transparent_union));
void t1 (union U);
void t1 (int *);
void t2 (int *);
void t2 (union { int *p; });
void t3 (const union { const int *p; } x);
void t3 (const int *const x) { }
/* still conflicting: a tagged union not transparent, a union named by a typedef, sizes that differ, no member of the
   type, transparent_union ignored (the first member's mode not the union's) */
union V { int *p; long *l; };
void r1 (union V);
void r1 (int *);			/* { dg-error "conflicting types" } */
typedef union { int *p; } N;
void r2 (N);
void r2 (int *);			/* { dg-error "conflicting types" } */
void r3 (union { char c[16]; int *p; });
void r3 (int *);			/* { dg-error "conflicting types" } */
void r4 (union { int *p; });
void r4 (long *);			/* { dg-error "conflicting types" } */
typedef union { char c; int *p; } I __attribute__ ((transparent_union));
void r5 (I);
void r5 (int *);			/* { dg-error "conflicting types" } */
/* transparent_union kept only when the first member's mode is the union's (compute_record_mode: a union takes its widest
   member's mode only when that is an integer mode, else the integer mode of its size) */
typedef union __attribute__ ((transparent_union)) { long l; double d; } K1;
void k1 (K1);
void k1 (double);
struct C8 { char a[8]; };
typedef union __attribute__ ((transparent_union)) { struct C8 s; long l; } K2;
void k2 (K2);
void k2 (long);
typedef union __attribute__ ((transparent_union)) { float f; int i; } D1;
void d1 (D1);
void d1 (int);				/* { dg-error "conflicting types" } */
typedef union __attribute__ ((transparent_union)) { float a; float b; } D2;
void d2 (D2);
void d2 (float);			/* { dg-error "conflicting types" } */
typedef union __attribute__ ((transparent_union)) { double d; long l; } D3;
void d3 (D3);
void d3 (long);				/* { dg-error "conflicting types" } */
struct F1 { float x; };
typedef union __attribute__ ((transparent_union)) { struct F1 s; int i; } D4;
void d4 (D4);
void d4 (int);				/* { dg-error "conflicting types" } */
