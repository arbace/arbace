/* { dg-do compile } */
/* CS214 R5: the main file's __extension__ on line 3 does not silence the header's pedwarn on its line 3. */
__extension__ typedef int T;
#include "ext-hdr-b.h"
