// The selftests' smallest program: no library, no exception (bin/cppi lower-check runs it in seconds).
int f(int x) { return x * 3 + 1; }
int main() { return f(2) == 7 ? 0 : 1; }
