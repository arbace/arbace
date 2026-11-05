/* hsi's glue to a program's own C (HES64 (a), doc/haskell/HASKELL-EVAL-SURVEY.md "As built: HE6's second half"): linked
   with the test's C files and musl by bin/hsi cnodes, so that musl's stdio flush and its allocator are in the node program
   whatever the test's C references. hsi calls __hsi_c_flush at the program's end (libc's exit flushes C's stdout after the
   Haskell handles' flush), and the Haskell side's malloc, calloc, realloc and free are musl's when a C program is loaded:
   a block may be allocated on one side and freed on the other. */
#include <stdio.h>
#include <stdlib.h>

void __hsi_c_flush(void) { fflush(0); }

void *(*const __hsi_c_alloc[])() = { (void *(*)())malloc, (void *(*)())calloc, (void *(*)())realloc, (void *(*)())free };
