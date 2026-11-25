/* CPython 3.14.10's Modules/_decimal/_decimal.c (the PSF's license, LICENSE.md) as a release build compiles it (queue 521):
   with NDEBUG, so its assert()s, which check CPython's own runtime state (context_new's PyObject_GC_IsTracked), are out, as
   in the interpreter the tests' references come from. The source is included unchanged; libmpdec's files, plain C, keep
   their assertions. */
#define NDEBUG 1
#include "Modules/_decimal/_decimal.c"
