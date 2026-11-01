// 7.26.2.2: memccpy in <string.h>.
#include <string.h>
void *until(void *dst, const void *src, size_t n) { return memccpy(dst, src, '\n', n); }
