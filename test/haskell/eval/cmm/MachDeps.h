/* hsi's MachDeps.h (doc/haskell/HASKELL-EVAL-SURVEY.md, "HES67's design: Cmm on hsi"): GHC 9.14.1's rts/include/MachDeps.h's
 * sizes for x86_64-linux, the machine hsi models (ghcautoconf.h's SIZEOF_ values as its configure gives them).
 * From GHC 9.14.1's source tarball: (c) The University of Glasgow 2002, under the Glasgow Haskell Compiler License (LICENSE.md). */

#pragma once

#define SIZEOF_CHAR 1
#define SIZEOF_SHORT 2
#define SIZEOF_INT 4
#define SIZEOF_LONG 8
#define SIZEOF_LONG_LONG 8
#define SIZEOF_VOID_P 8
#define SIZEOF_DOUBLE 8
#define SIZEOF_FLOAT 4
#define SIZEOF_HSCHAR 4
#define SIZEOF_HSINT SIZEOF_VOID_P
#define SIZEOF_HSWORD SIZEOF_VOID_P
#define SIZEOF_HSDOUBLE SIZEOF_DOUBLE
#define SIZEOF_HSFLOAT SIZEOF_FLOAT
#define SIZEOF_HSPTR SIZEOF_VOID_P
#define SIZEOF_INT64 8
#define SIZEOF_WORD64 8
#define WORD_SIZE_IN_BITS 64
#define WORD_SIZE_IN_BITS_FLOAT 64.0
