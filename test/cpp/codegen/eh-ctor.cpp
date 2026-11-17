// exceptions: a constructor that throws destroys the bases and members built so far; new's storage freed when the constructor throws
#include <cstdio>
struct Part { int id; Part(int i) : id(i) { printf("Part(%d)\n", id); if (id == 3) throw id; } ~Part() { printf("~Part(%d)\n", id); } };
struct Base { Part p; Base() : p(1) { printf("Base()\n"); } ~Base() { printf("~Base()\n"); } };
struct Whole : Base { Part a; Part b; Whole(int k) : a(2), b(k) { printf("Whole()\n"); } ~Whole() { printf("~Whole()\n"); } };
int main() {
  try { Whole w(4); printf("built\n"); } catch (int e) { printf("caught %d\n", e); }
  try { Whole w(3); printf("not reached\n"); } catch (int e) { printf("caught %d\n", e); }
  try { Whole* p = new Whole(3); delete p; } catch (int e) { printf("new caught %d\n", e); }
  Whole* q = new Whole(5); delete q;
  return 0;
}
