/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 057's finding 4: gcc's pedwarn on the bf16 suffix (not on f16). */
float a = 0.1bf16;	/* { dg-error "non-standard suffix on floating constant" } */
_Float16 b = 0.1f16;
float c = 1e3bf16;	/* { dg-error "non-standard suffix on floating constant" } */
