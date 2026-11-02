struct S;
int x;
int p1 = _Generic (x, struct S: 1, default: 2);
#pragma GCC system_header
int p2 = _Generic (x, struct S: 1, default: 2);
