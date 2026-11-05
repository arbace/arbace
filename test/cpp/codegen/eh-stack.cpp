// exceptions: a stack unwound through many frames, objects of every frame destroyed in order, the exception's
// destructor run after its handler, a handler that counts the frames
#include <cstdio>
static int live = 0;
struct Frame { int d; Frame(int d) : d(d) { live++; } ~Frame() { live--; printf("unwind %d\n", d); } };
struct Overflow { int depth; Overflow(int d) : depth(d) {} Overflow(const Overflow& o) : depth(o.depth) {} ~Overflow() { printf("~Overflow %d\n", depth); } };
int recurse(int d, int limit) {
  Frame f(d);
  if (d == limit) throw Overflow(d);
  return recurse(d + 1, limit) + 1;
}
int main() {
  try { recurse(0, 4); }
  catch (const Overflow& o) { printf("overflow at %d, live %d\n", o.depth, live); }
  printf("after, live %d\n", live);
  return 0;
}
