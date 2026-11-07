/* { dg-do compile } */
/* CS221 (a), CS232 (a): a declaration with conflicting specifier words (c/var, c/declare, c/typedef, a member, a parameter, a
   definition) prints its words as written, _BitInt with its width, so the reread keeps :c/bad-specs. */
long _BitInt(42) h;	/* { dg-error "both 'long' and '_BitInt'" } */
unsigned short _BitInt(7) *p, q;	/* { dg-error "both 'short' and '_BitInt'" } */
const long _Bool z;	/* { dg-error "two or more data types" } */
typedef int _BitInt(9) T;	/* { dg-error "both 'int' and '_BitInt'" } */
long _BitInt(8) f (void);	/* { dg-error "both 'long' and '_BitInt'" } */
struct S { short _BitInt(5) m; };	/* { dg-error "both 'short' and '_BitInt'" } */
void g (long _BitInt(6) a);	/* { dg-error "both 'long' and '_BitInt'" } */
long _BitInt(10);	/* { dg-error "both 'long' and '_BitInt'" } */
