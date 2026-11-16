/* Arbace's <stdarg.h> (ISO/IEC 9899:2024 §7.16), for x86_64-linux-musl (c/README.md): Arbace's own, over the GNU
   dialect's va builtins (the AMD64 ABI's va_list, §3.5.7, is the builtin type). */
#ifndef __ARBACE_STDARG_H
#define __ARBACE_STDARG_H

#if defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L
#define __STDC_VERSION_STDARG_H__ 202311L
#endif

#ifndef __DEFINED_va_list
typedef __builtin_va_list va_list;
#define __DEFINED_va_list
#endif
#ifndef __DEFINED___gnuc_va_list
typedef __builtin_va_list __gnuc_va_list;
#define __DEFINED___gnuc_va_list
#endif

/* C23 7.16.1.4: va_start takes the va_list and, optionally, the last named parameter (ignored) */
#if defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L
#define va_start(...) __builtin_c23_va_start(__VA_ARGS__)
#else
#define va_start(ap, last) __builtin_va_start(ap, last)
#endif
#define va_arg(ap, type) __builtin_va_arg(ap, type)
#define va_end(ap) __builtin_va_end(ap)
#if (defined __STDC_VERSION__ && __STDC_VERSION__ >= 199901L) || !defined __STRICT_ANSI__
#define va_copy(dest, src) __builtin_va_copy(dest, src)
#endif
#ifndef __STRICT_ANSI__
#define __va_copy(dest, src) __builtin_va_copy(dest, src)
#endif

#endif
