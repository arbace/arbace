struct S;
int x;
int a = _Generic (x, struct S: 1, default: 2);
