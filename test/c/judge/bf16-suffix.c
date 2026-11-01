/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 057's finding 4: gcc's pedwarn on the bf16 suffix (not on f16); review 079's finding 4: on the constants the preprocessor delivers
   (not in a skipped group or an unused macro, at the use of a macro's), and a constant without a leading digit. */
float a = 0.1bf16;	/* { dg-error "non-standard suffix on floating constant" } */
_Float16 b = 0.1f16;
float c = 1e3bf16;	/* { dg-error "non-standard suffix on floating constant" } */
float d = .5bf16;	/* { dg-error "non-standard suffix on floating constant" } */
float e = 0.1BF16;	/* { dg-error "non-standard suffix on floating constant" } */
#if 0
float f = 0.1bf16;
#endif
#define UNUSED 0.25bf16
#define USED 0.75bf16
float g = USED;		/* { dg-error "non-standard suffix on floating constant" } */
