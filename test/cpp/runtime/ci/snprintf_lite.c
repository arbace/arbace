// CX8 (doc/cpp/LLVM.md §11): libstdc++'s __gnu_cxx::__snprintf_lite (src/c++11/snprintf_lite.cc) for ci, in C. The lowered
// original reads its va_list as clang's -O0 IR does, by x86-64's __va_list_tag fields (gp_offset, reg_save_area) inline;
// ci's va_list is its own (a pointer), so on ci it read garbage (std::out_of_range's formatted message: bitset::test,
// basic_string::at). The lowered callers pass the va_list itself (LLVM.md §7), which vsnprintf takes. libstdc++'s formats
// here are %s, %zu and %% (its __throw_*_fmt), which vsnprintf writes alike; a message longer than the buffer is cut (the
// original throws; its callers size the buffer). Linked from the ci overrides library before the runtime (bin/cppi runtime-ci).
#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>

int _ZN9__gnu_cxx15__snprintf_liteEPcmPKcP13__va_list_tag(char *buf, size_t n, const char *fmt, va_list ap)
{
  int r = vsnprintf(buf, n, fmt, ap);
  return r < 0 || (size_t)r < n ? r : (int)n - 1;
}
