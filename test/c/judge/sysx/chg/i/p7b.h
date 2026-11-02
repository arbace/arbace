struct S;
int x;
int p7 = _Generic (x, struct S: 1, default: 2);
