/* pyconfig.h: Arbace's own (queue 244; doc/python/PYTHON-SURVEY.md, "As built: 244"): the configuration an extension
   module's C sees when ci builds it against Arbace's clean-room Python.h (this directory). The C target is ci's (x86-64,
   LP64, musl): a build with the GIL (Py_GIL_DISABLED undefined), wchar_t 4 bytes. Written from the C API's documentation
   (docs.python.org/3.14/c-api), not from CPython's pyconfig.h. */
#ifndef Py_PYCONFIG_H
#define Py_PYCONFIG_H
#define HAVE_WCHAR_H 1
#define SIZEOF_WCHAR_T 4
#define SIZEOF_VOID_P 8
#define SIZEOF_SIZE_T 8
#define SIZEOF_LONG 8
#define SIZEOF_LONG_LONG 8
#define SIZEOF_INT 4
#define SIZEOF_SHORT 2
#define SIZEOF_DOUBLE 8
#define SIZEOF_FLOAT 4
#define SIZEOF_LONG_DOUBLE 16
#endif
