/* Arbace's <stddef.h> (ISO/IEC 9899:2024 §7.21), for x86_64-linux-musl (c/README.md). Arbace's own, written from
   the standard and the predefined macros of c/target; the types share musl's guards (__DEFINED_T) so that musl's
   headers, which define them through <bits/alltypes.h>, do not define them again. */
#ifndef __ARBACE_STDDEF_H
#define __ARBACE_STDDEF_H

#if defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L
#define __STDC_VERSION_STDDEF_H__ 202311L
#endif

#undef NULL
#define NULL ((void *)0)

#ifndef __DEFINED_ptrdiff_t
typedef __PTRDIFF_TYPE__ ptrdiff_t;
#define __DEFINED_ptrdiff_t
#endif
#ifndef __DEFINED_size_t
typedef __SIZE_TYPE__ size_t;
#define __DEFINED_size_t
#endif
#ifndef __DEFINED_wchar_t
typedef __WCHAR_TYPE__ wchar_t;
#define __DEFINED_wchar_t
#endif

#if (defined __STDC_VERSION__ && __STDC_VERSION__ >= 201112L) || !defined __STRICT_ANSI__
#ifndef __DEFINED_max_align_t
/* the type of the greatest fundamental alignment: long double's, 16 (the AMD64 ABI, §3.1.2) */
typedef struct {
  long long __arbace_ll __attribute__((__aligned__(__alignof__(long long))));
  long double __arbace_ld __attribute__((__aligned__(__alignof__(long double))));
} max_align_t;
#define __DEFINED_max_align_t
#endif
#endif

#if defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L
typedef __typeof__(nullptr) nullptr_t;
#define unreachable() (__builtin_unreachable())
#endif

#define offsetof(TYPE, MEMBER) __builtin_offsetof(TYPE, MEMBER)

#endif
