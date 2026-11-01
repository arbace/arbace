// 6.7.11: the empty initializer.
struct s { int a; };
struct s v = {};
int a[3] = {};
int n = {};
void f(int k) { int vla[k] = {}; (void)vla; }
