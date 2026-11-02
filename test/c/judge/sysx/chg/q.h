struct S;
int x;
int q = _Generic (x, struct S: 1, default: 2);
