/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* The same header without the pragma, found through the quote path: the pedwarn is an error. */
#include "ext-hdr-q.h"
