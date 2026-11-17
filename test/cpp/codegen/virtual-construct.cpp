// virtual inheritance during construction and destruction: virtual calls from the bases' constructors and
// destructors (the construction vtables of the VTT), a virtual base's members seen from a base's constructor
#include <cstdio>
struct V { int tag; V() : tag(1) { printf("V sees %s\n", who()); } virtual ~V() { printf("~V sees %s\n", who()); } virtual const char* who() const { return "V"; } };
struct A : virtual V { A() { tag += 10; printf("A sees %s, tag %d\n", who(), tag); } ~A() { printf("~A sees %s\n", who()); } const char* who() const override { return "A"; } };
struct B : virtual V { B() { tag += 100; printf("B sees %s, tag %d\n", who(), tag); } ~B() { printf("~B sees %s\n", who()); } };
struct C : A, B { C() { printf("C sees %s, tag %d\n", who(), tag); } ~C() { printf("~C sees %s\n", who()); } const char* who() const override { return "C"; } };
int main() {
  C c;
  V& v = c;
  printf("%s %d\n", v.who(), v.tag);
  return 0;
}
