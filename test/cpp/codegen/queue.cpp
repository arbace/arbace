// a ring buffer class with a member array, const and non-const accessors overloaded, a friend-free iteration
#include <cstdio>
class Ring {
  int buf[5];
  int head = 0, count = 0;
public:
  bool push(int v) { if (count == 5) return false; buf[(head + count++) % 5] = v; return true; }
  bool pop(int &out) { if (!count) return false; out = buf[head]; head = (head + 1) % 5; --count; return true; }
  int &at(int i) { return buf[(head + i) % 5]; }
  int at(int i) const { return buf[(head + i) % 5] + 1000; }
  int size() const { return count; }
};
int peek(const Ring &r) { return r.at(0); }
int main() {
  Ring r;
  for (int i = 0; i < 7; i++) printf("%d", r.push(i * 3));
  printf("\n");
  int v;
  r.pop(v); r.pop(v);
  printf("%d %d\n", v, r.size());
  r.push(100); r.push(200);
  r.at(0) = -1;
  for (int i = 0; i < r.size(); i++) printf("%d ", r.at(i));
  printf("\n%d\n", peek(r));
  return 0;
}
