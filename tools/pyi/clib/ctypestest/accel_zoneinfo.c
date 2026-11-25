/* CPython 3.14.10's Modules/_zoneinfo.c (the PSF's license, LICENSE.md) as a release build compiles it (py-accel, after
   queue 521's _datetime and _decimal): with NDEBUG, the source included unchanged; its datetime C API is the world's
   _datetime's (PyDateTime_IMPORT's capsule). */
#define NDEBUG 1
#include "Modules/_zoneinfo.c"
