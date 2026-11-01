/* { dg-do compile } */
/* { dg-options "-std=c23" } */
/* Review 057's finding 4: _Float16 with __bf16 is _Float16 in either order (gcc 15). */
_Static_assert(_Generic((__bf16)1 + (_Float16)1, _Float16: 1, default: 0));
_Static_assert(_Generic((_Float16)1 + (__bf16)1, _Float16: 1, default: 0));
