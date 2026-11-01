// 6.7.7.4, 6.9.2: an attribute after a function definition's declarator.
int f(void) [[unsequenced]] { return 1; }
