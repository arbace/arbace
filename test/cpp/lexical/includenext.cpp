// { dg-options "-std=c++17" }
// libstdc++'s <stdlib.h> and <cstdlib> reach the C library's by #include_next, through the fortify wrapper
#include <stdlib.h>
#include <math.h>
#include <cmath>
#include <limits.h>
#include <string.h>
int x = EXIT_SUCCESS;
