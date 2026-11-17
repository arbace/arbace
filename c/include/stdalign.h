/* Arbace's <stdalign.h> (ISO/IEC 9899:2024 §7.15; C17 §7.15), c/README.md. */
#ifndef __ARBACE_STDALIGN_H
#define __ARBACE_STDALIGN_H
#if !(defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L)
/* C23 makes alignas and alignof keywords */
#define alignas _Alignas
#define alignof _Alignof
#endif
#define __alignas_is_defined 1
#define __alignof_is_defined 1
#endif
