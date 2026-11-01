// 7.16.1.4: va_start with one argument (stdarg.h's macro is gcc's builtin).
void f(int n, ...) { __builtin_va_list ap; __builtin_c23_va_start(ap); __builtin_va_end(ap); }
