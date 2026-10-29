// 6.7.6, 6.5.4.4: alignas and alignof are keywords.
alignas(16) int x;
alignas(long) int y;
int z = alignof(int);
struct s { alignas(8) char c; };
