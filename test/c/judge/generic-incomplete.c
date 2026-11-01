/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 027's finding 2: an incomplete or a function type as a _Generic association is C2y's; gcc 15 pedwarns before C2y. */
struct S;
int x;
int a = _Generic (x, struct S: 1, default: 2);		/* { dg-error "association with incomplete type before C2Y" } */
int b = _Generic (x, void: 1, default: 2);		/* { dg-error "association with incomplete type before C2Y" } */
int c = _Generic (x, int (): 1, default: 2);		/* { dg-error "association with function type before C2Y" } */
int d = _Generic (x, int: 1, default: 2);
