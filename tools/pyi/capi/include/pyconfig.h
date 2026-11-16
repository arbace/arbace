/* pyconfig.h: Arbace's own (queues 244, 262; doc/python/PYTHON-SURVEY.md, "As built: 244", "As built: 262"): the
   configuration CPython 3.14.10's Include/ reads when ci builds an extension module of pyi's C world against it in
   the limited API's mode (configure's output for this target, written by hand: the macros Include/ consults). The C
   target is ci's (x86-64, LP64, musl): a build with the GIL (Py_GIL_DISABLED undefined), wchar_t 4 bytes, pid_t an
   int. Since queue 269 also the full API's (_testcapi's parts define no Py_LIMITED_API): its headers ask for POSIX
   threads (cpython/pythread.h's Py_tss_t) and fork. */
#ifndef Py_PYCONFIG_H
#define Py_PYCONFIG_H
#define HAVE_WCHAR_H 1
#define HAVE_PTHREAD_H 1
#define HAVE_FORK 1
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
/* queue 269: gcc's __atomic builtins (Include/cpython/pyatomic_gcc.h, which the full API's inline functions call):
   ci's builder implements none of them (a call faults as an undefined function) and its checker takes the generic
   __atomic_load and __atomic_store for prototyped functions; ci's world is one thread, so each is its plain access
   (statement expressions and __typeof__, which ci reads). A REQUEST for the C crop (arbace/c's builtins). */
#define __atomic_load(p, r, o) ((void)(o), *(r) = *(p))
#define __atomic_store(p, v, o) ((void)(o), *(p) = *(v))
#define __atomic_load_n(p, o) ((void)(o), *(p))
#define __atomic_store_n(p, v, o) ((void)(o), (void)(*(p) = (v)))
#define __atomic_thread_fence(o) ((void)(o))
#define __atomic_fetch_add(p, v, o) ({ __typeof__(*(p)) _pyi_o = *(p); *(p) += (v); (void)(o); _pyi_o; })
#define __atomic_fetch_and(p, v, o) ({ __typeof__(*(p)) _pyi_o = *(p); *(p) &= (v); (void)(o); _pyi_o; })
#define __atomic_fetch_or(p, v, o) ({ __typeof__(*(p)) _pyi_o = *(p); *(p) |= (v); (void)(o); _pyi_o; })
#define __atomic_exchange_n(p, v, o) ({ __typeof__(*(p)) _pyi_o = *(p); *(p) = (v); (void)(o); _pyi_o; })
#define __atomic_compare_exchange_n(p, e, d, w, s, f) \
    ({ int _pyi_r = *(p) == *(e); if (_pyi_r) *(p) = (d); else *(e) = *(p); _pyi_r; })
#endif
