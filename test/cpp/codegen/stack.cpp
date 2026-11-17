// a class with a dynamic array: copy constructor, copy assignment, destructor (the rule of three)
#include <cstdio>
class Stack {
  int *data;
  int cap, top;
  void grow() { int *n = new int[cap * 2]; for (int i = 0; i < top; i++) n[i] = data[i]; delete[] data; data = n; cap *= 2; }
public:
  Stack(int c = 2) : data(new int[c]), cap(c), top(0) {}
  Stack(const Stack &o) : data(new int[o.cap]), cap(o.cap), top(o.top) { for (int i = 0; i < top; i++) data[i] = o.data[i]; printf("copied %d\n", top); }
  Stack &operator=(const Stack &o) {
    if (this == &o) return *this;
    delete[] data; data = new int[o.cap]; cap = o.cap; top = o.top;
    for (int i = 0; i < top; i++) data[i] = o.data[i];
    printf("assigned %d\n", top);
    return *this;
  }
  ~Stack() { printf("destroyed %d of %d\n", top, cap); delete[] data; }
  void push(int v) { if (top == cap) grow(); data[top++] = v; }
  int pop() { return data[--top]; }
  bool empty() const { return top == 0; }
  int size() const { return top; }
};
int drain(Stack s) { int t = 0; while (!s.empty()) t += s.pop(); return t; }
int main() {
  Stack s;
  for (int i = 1; i <= 9; i++) s.push(i * i);
  Stack c = s;
  printf("%d %d\n", c.pop(), c.size());
  Stack d(1);
  d = c;
  d = d;
  printf("%d %d %d\n", drain(d), d.size(), s.size());
  return 0;
}
