/* CPython 3.14.10's Modules/_json.c (the PSF's license, LICENSE.md) as a release build compiles it (py-json-1126, after
   queue 568's _pickle and queue 521's _datetime and _decimal): with NDEBUG, the source included unchanged. Built as
   CPython builds it on Linux, an extension module (Py_BUILD_CORE_MODULE); the functions it calls are arbace/pyctypes's
   natives, and the identifiers and the literal it names in _PyRuntime (_Py_ID, _Py_STR) are bound there. */
#define NDEBUG 1
#include "Modules/_json.c"
