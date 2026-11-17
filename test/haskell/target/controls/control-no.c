// HFS-I12 (a): the probe must answer "no" to this compile test (a conflicting declaration, as the probe's own compiles checks are made)
#include <stdlib.h>
void unsetenv(const char *);
