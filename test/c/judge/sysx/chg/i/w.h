struct S;
int x;
int w = _Generic (x, struct S: 1, default: 2);
