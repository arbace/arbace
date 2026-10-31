/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* CS194 (a): C2y's if and switch declarations, read and checked (the pedwarn before C2y). */
int
f (int n)
{
  int r = 0;
  if (int i = n) r += i;			/* { dg-error "ISO C does not support if declarations before C2Y" } */
  if (int i = 0, j = n; i + j > 3) r += j;	/* { dg-error "ISO C does not support if declarations before C2Y" } */
  else r -= j;
  switch (int s = n; s)				/* { dg-error "ISO C does not support if declarations before C2Y" } */
    {
    case 3: r += s; break;
    default: break;
    }
  return r + i;					/* { dg-error "'i' undeclared" } */
}
