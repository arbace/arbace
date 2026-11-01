// 6.10.1: __has_include in #if and #elif.
#if __has_include(<stddef.h>)
#include <stddef.h>
typedef size_t count_t;
#elif __has_include("nowhere.h")
#include "nowhere.h"
#endif
#if !__has_include(<no/such/header.h>)
count_t n = 0;
#endif
