struct S;
int x;
int o1 = _Generic (x, struct S: 1, default: 2);
_Pragma ("GCC system_header")
int o2 = _Generic (x, struct S: 1, default: 2);
