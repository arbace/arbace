/* { dg-do compile } */
/* Review 031's finding G: the qualifiers of a declaration with invalid specifier words print back with the words. */
const int bool;	/* { dg-error "two or more data types" } */
volatile const char bool;	/* { dg-error "two or more data types" } */
_Atomic int bool;	/* { dg-error "two or more data types" } */
const _Atomic int bool;	/* { dg-error "two or more data types" } */
