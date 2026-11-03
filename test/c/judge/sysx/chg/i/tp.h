#ifdef TP_SYS
#pragma GCC system_header
#endif
struct S;
int x;
_Static_assert (_Generic (x, struct S: 1, default: 1), "tp");
