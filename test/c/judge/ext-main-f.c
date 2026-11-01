/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors -isystem sys" } */
/* CS231: a header found through -isystem is a system header: no pedwarn in it (with -I sys instead, the error). */
#include <ext-hdr-s.h>
