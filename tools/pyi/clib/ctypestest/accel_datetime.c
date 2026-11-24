/* CPython 3.14.10's Modules/_datetimemodule.c (the PSF's license, LICENSE.md) as a release build compiles it (queue 521):
   with NDEBUG, so its assert()s, which check CPython's own runtime state (a GC-tracked object, a refcount), are out, as in
   the interpreter the tests' references come from. The source is included unchanged. */
#define NDEBUG 1
#include "Modules/_datetimemodule.c"
