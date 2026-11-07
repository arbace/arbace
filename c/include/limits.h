/* Arbace's <limits.h> (ISO/IEC 9899:2024 §5.2.5.2, §7.10), for x86_64-linux-musl (c/README.md): the implementation's
   limits from the predefined macros of c/target, after musl's <limits.h> (POSIX's limits) when hosted. */
#ifndef __ARBACE_LIMITS_H
#define __ARBACE_LIMITS_H

#if __STDC_HOSTED__
#include_next <limits.h>
#endif

#undef CHAR_BIT
#define CHAR_BIT __CHAR_BIT__
#ifndef MB_LEN_MAX
#define MB_LEN_MAX 1
#endif

#undef SCHAR_MIN
#define SCHAR_MIN (-SCHAR_MAX - 1)
#undef SCHAR_MAX
#define SCHAR_MAX __SCHAR_MAX__
#undef UCHAR_MAX
#define UCHAR_MAX (SCHAR_MAX * 2 + 1)
#undef CHAR_MIN
#undef CHAR_MAX
#ifdef __CHAR_UNSIGNED__
#define CHAR_MIN 0
#define CHAR_MAX UCHAR_MAX
#else
#define CHAR_MIN SCHAR_MIN
#define CHAR_MAX SCHAR_MAX
#endif

#undef SHRT_MIN
#define SHRT_MIN (-SHRT_MAX - 1)
#undef SHRT_MAX
#define SHRT_MAX __SHRT_MAX__
#undef USHRT_MAX
#define USHRT_MAX (SHRT_MAX * 2 + 1)

#undef INT_MIN
#define INT_MIN (-INT_MAX - 1)
#undef INT_MAX
#define INT_MAX __INT_MAX__
#undef UINT_MAX
#define UINT_MAX (INT_MAX * 2U + 1U)

#undef LONG_MIN
#define LONG_MIN (-LONG_MAX - 1L)
#undef LONG_MAX
#define LONG_MAX __LONG_MAX__
#undef ULONG_MAX
#define ULONG_MAX (LONG_MAX * 2UL + 1UL)

#if (defined __STDC_VERSION__ && __STDC_VERSION__ >= 199901L) || !defined __STRICT_ANSI__
#undef LLONG_MIN
#define LLONG_MIN (-LLONG_MAX - 1LL)
#undef LLONG_MAX
#define LLONG_MAX __LONG_LONG_MAX__
#undef ULLONG_MAX
#define ULLONG_MAX (LLONG_MAX * 2ULL + 1ULL)
#endif

#if !defined __STRICT_ANSI__
/* the GNU spellings */
#undef LONG_LONG_MIN
#define LONG_LONG_MIN (-LONG_LONG_MAX - 1LL)
#undef LONG_LONG_MAX
#define LONG_LONG_MAX __LONG_LONG_MAX__
#undef ULONG_LONG_MAX
#define ULONG_LONG_MAX (LONG_LONG_MAX * 2ULL + 1ULL)
#endif

/* C23 5.2.5.2: the widths, bool's, the widest _BitInt (also for TS 18661-1's request before C23) */
#if (defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L) || defined __STDC_WANT_IEC_60559_BFP_EXT__
#undef CHAR_WIDTH
#define CHAR_WIDTH __SCHAR_WIDTH__
#undef SCHAR_WIDTH
#define SCHAR_WIDTH __SCHAR_WIDTH__
#undef UCHAR_WIDTH
#define UCHAR_WIDTH __SCHAR_WIDTH__
#undef SHRT_WIDTH
#define SHRT_WIDTH __SHRT_WIDTH__
#undef USHRT_WIDTH
#define USHRT_WIDTH __SHRT_WIDTH__
#undef INT_WIDTH
#define INT_WIDTH __INT_WIDTH__
#undef UINT_WIDTH
#define UINT_WIDTH __INT_WIDTH__
#undef LONG_WIDTH
#define LONG_WIDTH __LONG_WIDTH__
#undef ULONG_WIDTH
#define ULONG_WIDTH __LONG_WIDTH__
#undef LLONG_WIDTH
#define LLONG_WIDTH __LONG_LONG_WIDTH__
#undef ULLONG_WIDTH
#define ULLONG_WIDTH __LONG_LONG_WIDTH__
#endif
#if defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L
#undef BOOL_MAX
#define BOOL_MAX 1
#undef BOOL_WIDTH
#define BOOL_WIDTH 1
#define __STDC_VERSION_LIMITS_H__ 202311L
#ifdef __BITINT_MAXWIDTH__
#undef BITINT_MAXWIDTH
#define BITINT_MAXWIDTH __BITINT_MAXWIDTH__
#endif
#endif

#endif
