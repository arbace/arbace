// 7.26.2.6, 7.26.2.7: strdup and strndup in <string.h>.
#include <string.h>
#include <stdlib.h>
char *copy_prefix(const char *s, size_t n) { return n ? strndup(s, n) : strdup(s); }
