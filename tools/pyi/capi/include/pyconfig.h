/* pyconfig.h: Arbace's own (queues 244, 262; doc/python/PYTHON-SURVEY.md, "As built: 244", "As built: 262"): the
   configuration CPython 3.14.10's Include/ reads when ci builds an extension module of pyi's C world against it in
   the limited API's mode (configure's output for this target, written by hand: the macros Include/ consults). The C
   target is ci's (x86-64, LP64, musl): a build with the GIL (Py_GIL_DISABLED undefined), wchar_t 4 bytes, pid_t an
   int. */
#ifndef Py_PYCONFIG_H
#define Py_PYCONFIG_H
#define HAVE_WCHAR_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_UNISTD_H 1
#define HAVE_LONG_LONG 1
#define HAVE_SSIZE_T 1
#define HAVE_USABLE_WCHAR_T 0
#define SIZEOF_WCHAR_T 4
#define SIZEOF_VOID_P 8
#define SIZEOF_SIZE_T 8
#define SIZEOF_UINTPTR_T 8
#define SIZEOF_LONG 8
#define SIZEOF_LONG_LONG 8
#define SIZEOF_INT 4
#define SIZEOF_SHORT 2
#define SIZEOF_DOUBLE 8
#define SIZEOF_FLOAT 4
#define SIZEOF_LONG_DOUBLE 16
#define SIZEOF_PID_T 4
#define SIZEOF_TIME_T 8
#define SIZEOF__BOOL 1
#define ALIGNOF_MAX_ALIGN_T 16
#define WITH_DOC_STRINGS 1
#endif
