struct S;
int x;
int f1 = _Generic (x, struct S: 1, default: 2);
#if 0
#pragma GCC system_header
#endif
/*
#pragma GCC system_header
*/
int f2 = _Generic (x, struct S: 1, default: 2);
