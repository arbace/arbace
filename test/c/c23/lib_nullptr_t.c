// 7.21: nullptr_t, the type of nullptr, a typedef of <stddef.h>.
#include <stddef.h>
nullptr_t none = nullptr;
int is_null(void *p) { return p == none; }
