struct S;
int x;
int lmc = _Generic (x, struct S: 1, default: 2);
