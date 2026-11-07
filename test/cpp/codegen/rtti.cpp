// RTTI: typeid of a polymorphic object (the dynamic type), of a type and of a non-polymorphic expression, type_info's
// name, ==, != and before; dynamic_cast down, across and to void*, a failing pointer cast and a failing reference cast
#include <cstdio>
#include <typeinfo>
struct Animal { virtual ~Animal() {} virtual const char* sound() const { return "..."; } };
struct Dog : Animal { const char* sound() const override { return "woof"; } void fetch() const { printf("fetching\n"); } };
struct Cat : Animal { const char* sound() const override { return "meow"; } };
struct Pet { virtual ~Pet() {} int age = 3; };
struct Puppy : Dog, Pet {};
struct Plain { int x; };
void describe(Animal* a) {
  if (Dog* d = dynamic_cast<Dog*>(a)) { printf("a dog: %s ", d->sound()); d->fetch(); }
  else if (dynamic_cast<Cat*>(a)) printf("a cat: %s\n", a->sound());
  else printf("an animal\n");
}
int main() {
  Animal* zoo[] = {new Dog, new Cat, new Animal, new Puppy};
  for (Animal* a : zoo) describe(a);
  printf("%s %s %s\n", typeid(*zoo[0]).name(), typeid(*zoo[1]).name(), typeid(Animal).name());
  printf("%d %d %d\n", typeid(*zoo[0]) == typeid(Dog), typeid(*zoo[1]) != typeid(Dog), typeid(int) == typeid(const int));
  Plain p{1};
  printf("%s %s %d\n", typeid(p).name(), typeid(double).name(), typeid(Dog).before(typeid(Cat)) != typeid(Cat).before(typeid(Dog)));
  Pet* pet = dynamic_cast<Pet*>(zoo[3]);
  printf("pet age %d, back %d\n", pet->age, dynamic_cast<Animal*>(pet) == zoo[3]);
  printf("void* %d\n", dynamic_cast<void*>(pet) == dynamic_cast<void*>(zoo[3]));
  printf("none %d\n", dynamic_cast<Pet*>(zoo[0]) == nullptr);
  Animal* nul = nullptr;
  printf("null %d\n", dynamic_cast<Dog*>(nul) == nullptr);
  try { Cat& c = dynamic_cast<Cat&>(*zoo[0]); printf("not reached %s\n", c.sound()); }
  catch (...) { printf("bad_cast thrown\n"); }
  Dog& d = dynamic_cast<Dog&>(*zoo[3]);
  d.fetch();
  for (Animal* a : zoo) delete a;
  return 0;
}
