/* Arbace's <stdbool.h> (ISO/IEC 9899:2024 §7.19; before C23, C17 §7.18), c/README.md. */
#ifndef __ARBACE_STDBOOL_H
#define __ARBACE_STDBOOL_H
#if !(defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L)
/* C23 makes bool, true and false keywords; before it they are this header's macros */
#define bool _Bool
#define true 1
#define false 0
#endif
#define __bool_true_false_are_defined 1
#endif
