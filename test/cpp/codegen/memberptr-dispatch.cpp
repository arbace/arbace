// pointers to member functions as a dispatch table, passed to and returned from functions, a pointer to a base's member
// used on a derived object, a pointer to a virtual function called on objects of different classes
#include <cstdio>
struct Machine {
  int acc = 0;
  void inc(int k) { acc += k; }
  void dbl(int) { acc *= 2; }
  void neg(int) { acc = -acc; }
};
typedef void (Machine::*Instr)(int);
Instr decode(char c) { return c == '+' ? &Machine::inc : c == '*' ? &Machine::dbl : &Machine::neg; }
void run(Machine& m, Instr i, int k) { (m.*i)(k); }
struct Animal { virtual ~Animal() {} virtual int legs() const { return 0; } };
struct Bird : Animal { int legs() const override { return 2; } };
struct Dog : Animal { int legs() const override { return 4; } };
int main() {
  Machine m;
  const char* prog = "++*-+";
  for (const char* p = prog; *p; p++) { run(m, decode(*p), 3); printf("%d ", m.acc); }
  printf("\n");
  int (Animal::*legs)() const = &Animal::legs;
  Bird b; Dog d; Animal a;
  Animal* zoo[] = {&a, &b, &d};
  for (Animal* z : zoo) printf("%d ", (z->*legs)());
  printf("\n");
  int Machine::*accp = &Machine::acc;
  m.*accp = 100;
  printf("%d\n", m.acc);
  return 0;
}
