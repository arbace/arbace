struct S;
int x;
int a = __extension__ _Generic (x, struct S: 1, default: 2);
