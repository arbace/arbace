// 6.7.13.8: the function type attributes [[unsequenced]] and [[reproducible]].
int sq(int x) [[unsequenced]];
int rd(const int *p) [[reproducible]];
