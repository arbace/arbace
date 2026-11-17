// RTTI with multiple inheritance: dynamic_cast across from one base to the other, typeid through a secondary base,
// dynamic_cast to void* from either base, a failed cross cast
#include <cstdio>
#include <typeinfo>
struct Readable { virtual ~Readable() {} virtual int r() const { return 1; } };
struct Writable { virtual ~Writable() {} virtual int w() const { return 2; } };
struct File : Readable, Writable { int r() const override { return 10; } int w() const override { return 20; } };
struct Pipe : Readable {};
int main() {
  File f; Pipe p;
  Readable* rs[] = {&f, &p};
  for (Readable* r : rs) {
    Writable* w = dynamic_cast<Writable*>(r);
    printf("%s r %d w %d\n", typeid(*r).name(), r->r(), w ? w->w() : -1);
  }
  Writable* wf = &f;
  printf("%s %d\n", typeid(*wf).name(), typeid(*wf) == typeid(File));
  printf("%d\n", dynamic_cast<void*>(wf) == dynamic_cast<void*>(static_cast<Readable*>(&f)));
  File* back = dynamic_cast<File*>(wf);
  printf("%d\n", back == &f);
  return 0;
}
