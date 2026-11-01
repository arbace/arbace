/* { dg-do compile } */
/* { dg-options "-std=c23 -pedantic-errors" } */
/* Review 031's finding F: a header with #pragma GCC system_header is a system header, whatever way it was found: no pedwarn in it. */
#include "ext-hdr-p.h"
