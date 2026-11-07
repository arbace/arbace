// virtual functions with the same return type through two hierarchies, a clone pattern returning the base pointer,
// overloads of virtual functions, a virtual function hidden and called by qualification
#include <cstdio>
struct Shape { virtual ~Shape() {} virtual Shape* clone() const = 0; virtual int sides() const = 0; virtual int scale(int k) const { return k; } virtual int scale(double d) const { return (int)(d * 2); } };
struct Tri : Shape { Shape* clone() const override { return new Tri(*this); } int sides() const override { return 3; } int scale(int k) const override { return k * 3; } };
struct Quad : Shape { int w = 2; Shape* clone() const override { return new Quad(*this); } int sides() const override { return 4; } };
int main() {
  Shape* shapes[] = {new Tri, new Quad};
  for (Shape* s : shapes) {
    Shape* c = s->clone();
    printf("%d %d %d %d\n", c->sides(), c->scale(2), c->scale(1.5), c->Shape::scale(2));
    delete c;
  }
  for (Shape* s : shapes) delete s;
  return 0;
}
