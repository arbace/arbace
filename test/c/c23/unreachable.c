// 7.21.1: unreachable() (stddef.h's macro is gcc's builtin).
int f(int x) { if (x) { return 1; } __builtin_unreachable(); }
