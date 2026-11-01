// 6.7.13.6, 6.8.1: [[fallthrough]] as an attribute declaration in a statement's place.
int f(int x) { switch (x) { case 1: x++; [[fallthrough]]; default: break; } return x; }
