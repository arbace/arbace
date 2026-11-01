// 6.8.2: a case label before a declaration.
int f(int x) { switch (x) { case 1: int y = 2; return y; default: return 0; } }
