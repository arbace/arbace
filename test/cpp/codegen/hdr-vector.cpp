// G4: std::vector<int> from <vector>: push_back (its reallocation through allocator_traits, construct_at's placement
// new, __uninitialized_move_if_noexcept_a, the constrained iterator_traits<T*>), size, operator[], back, iteration by
// index, by iterator (__normal_iterator's operators, C++20's rewritten !=) and by a range for
#include <cstdio>
#include <vector>
int main() {
  std::vector<int> v;
  for (int i = 0; i < 5; ++i) v.push_back(i * i);
  int a = 0;
  for (unsigned i = 0; i < v.size(); ++i) a += v[i];
  int b = 0;
  for (auto it = v.begin(); it != v.end(); ++it) b += *it;
  int c = 0;
  for (int x : v) c += x;
  v[0] = 7;
  std::printf("%d %d %d %d %d %d\n", (int)v.size(), a, b, c, v.back(), v[0]);
  return 0;
}
