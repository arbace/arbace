// a polymorphic hierarchy in an array of base pointers, a virtual call from a base method, deleting through the base
#include <cstdio>
class Animal {
protected:
  const char *name;
  int legs;
public:
  Animal(const char *n, int l) : name(n), legs(l) {}
  virtual ~Animal() { printf("bye %s\n", name); }
  virtual const char *sound() const { return "..."; }
  virtual int speed() const { return legs * 2; }
  void report() const { printf("%s says %s, speed %d\n", name, sound(), speed()); }
};
class Dog : public Animal { public: Dog() : Animal("dog", 4) {} const char *sound() const override { return "woof"; } };
class Bird : public Animal {
  int wings;
public:
  Bird(const char *n) : Animal(n, 2), wings(2) {}
  const char *sound() const override { return "tweet"; }
  int speed() const override { return Animal::speed() * 5 + wings; }
};
class Penguin : public Bird { public: Penguin() : Bird("penguin") {} int speed() const override { return 1; } };
int main() {
  Animal *zoo[4] = {new Dog, new Bird("robin"), new Penguin, new Animal("snail", 0)};
  int fastest = 0;
  for (int i = 0; i < 4; i++) { zoo[i]->report(); if (zoo[i]->speed() > zoo[fastest]->speed()) fastest = i; }
  printf("fastest %d\n", fastest);
  for (int i = 3; i >= 0; i--) delete zoo[i];
  return 0;
}
