// recursion: factorial, fibonacci with memo, mutual recursion, the towers of Hanoi, a recursive struct walk
#include <cstdio>
long fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); }
long memo[100];
long fib(int n) { if (n < 2) return n; if (memo[n]) return memo[n]; return memo[n] = fib(n - 1) + fib(n - 2); }
bool is_odd(int n);
bool is_even(int n) { return n == 0 ? true : is_odd(n - 1); }
bool is_odd(int n) { return n == 0 ? false : is_even(n - 1); }
int moves = 0;
void hanoi(int n, char from, char to, char via) { if (n == 0) return; hanoi(n - 1, from, via, to); ++moves; hanoi(n - 1, via, to, from); }
struct Tree { int v; Tree *l, *r; };
int depth(const Tree *t) { if (!t) return 0; int a = depth(t->l), b = depth(t->r); return 1 + (a > b ? a : b); }
int total(const Tree *t) { return t ? t->v + total(t->l) + total(t->r) : 0; }
int main() {
  printf("%ld %ld\n", fact(10), fact(20));
  printf("%ld %ld\n", fib(50), fib(90));
  printf("%d %d\n", is_even(10), is_odd(7));
  hanoi(10, 'a', 'c', 'b');
  printf("%d\n", moves);
  Tree d{4, nullptr, nullptr}, c{3, &d, nullptr}, b{2, nullptr, nullptr}, a{1, &b, &c};
  printf("%d %d\n", depth(&a), total(&a));
  return 0;
}
