struct S;
int x;
int lpc = _Generic (x, struct S: 1, default: 2);
