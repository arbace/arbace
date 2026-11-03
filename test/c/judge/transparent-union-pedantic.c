/* { dg-do compile } */
/* { dg-options "-std=gnu23 -pedantic-errors" } */
/* Queue 158: the composite of a redeclaration through a union parameter is -Wpedantic's pedwarn (composite_type). */
union U { int *p; long *l; } __attribute__ ((transparent_union));
void t1 (union U);
void t1 (int *);			/* { dg-error "not truly compatible" } */
void t2 (int *);
void t2 (union { int *p; });		/* { dg-error "not truly compatible" } */
union V { int *p; long *l; };
void r1 (union V);
void r1 (int *);			/* { dg-error "conflicting types" } */
