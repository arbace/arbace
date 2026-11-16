// virtual inheritance: a diamond with one shared base, the most derived class initializing it, construction and
// destruction order, virtual calls through each base (virtual thunks), the base's members through either path, sizeof
#include <cstdio>
struct Base {
  int id;
  Base(int i) : id(i) { printf("Base(%d)\n", id); }
  virtual ~Base() { printf("~Base %d\n", id); }
  virtual const char* name() const { return "Base"; }
  virtual int weight() const { return id; }
};
struct Left : virtual Base { int l = 1; Left() : Base(-1) { printf("Left\n"); } ~Left() { printf("~Left\n"); } const char* name() const override { return "Left"; } };
struct Right : virtual Base { int r = 2; Right() : Base(-2) { printf("Right\n"); } ~Right() { printf("~Right\n"); } int weight() const override { return id * 10 + r; } };
struct Join : Left, Right {
  int j = 3;
  Join() : Base(7) { printf("Join\n"); }
  ~Join() { printf("~Join\n"); }
  const char* name() const override { return "Join"; }
};
void show(const Base& b) { printf("%s weighs %d (id %d)\n", b.name(), b.weight(), b.id); }
int main() {
  {
    Join j;
    show(j);
    Left& l = j; Right& r = j;
    printf("%s %d %d %d %d\n", l.name(), r.weight(), l.id, r.id, j.id);
    l.id = 9;
    printf("%d %d\n", r.id, static_cast<Base&>(j).id);
    printf("sizes %zu %zu %zu %zu\n", sizeof(Base), sizeof(Left), sizeof(Right), sizeof(Join));
  }
  Base* b = new Join;
  show(*b);
  delete b;
  Left solo;
  show(solo);
  return 0;
}
