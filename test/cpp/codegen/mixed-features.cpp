// the G2 features together: a polymorphic expression tree with a virtual base for node identity, member function
// pointers for the operators, lambdas for evaluation, exceptions for division by zero, unions for literal values
#include <cstdio>
struct Ident { int id; Ident() : id(++next) {} virtual ~Ident() {} static int next; };
int Ident::next = 0;
struct DivZero { int at; };
struct Expr : virtual Ident { virtual double eval() const = 0; virtual void show() const = 0; };
struct Lit : Expr {
  bool isInt; union { long i; double d; };
  Lit(long v) : isInt(true), i(v) {} Lit(double v) : isInt(false), d(v) {}
  double eval() const override { return isInt ? (double)i : d; }
  void show() const override { if (isInt) printf("%ld", i); else printf("%.1f", d); }
};
struct Bin : Expr {
  char op; Expr* l; Expr* r;
  double (Bin::*fn)(double, double) const;
  double add(double a, double b) const { return a + b; }
  double mul(double a, double b) const { return a * b; }
  double div(double a, double b) const { if (b == 0) throw DivZero{id}; return a / b; }
  Bin(char o, Expr* a, Expr* b) : op(o), l(a), r(b), fn(o == '+' ? &Bin::add : o == '*' ? &Bin::mul : &Bin::div) {}
  ~Bin() override { delete l; delete r; }
  double eval() const override { return (this->*fn)(l->eval(), r->eval()); }
  void show() const override { printf("("); l->show(); printf(" %c ", op); r->show(); printf(")"); }
};
int main() {
  Expr* exprs[] = {new Bin('+', new Lit(2L), new Bin('*', new Lit(3.5), new Lit(4L))),
                   new Bin('/', new Lit(1L), new Bin('+', new Lit(-2L), new Lit(2L)))};
  auto run = [](const Expr* e) {
    e->show();
    try { printf(" = %.2f\n", e->eval()); } catch (const DivZero& z) { printf(" divides by zero at node %d\n", z.at); }
  };
  for (Expr* e : exprs) run(e);
  for (Expr* e : exprs) delete e;
  printf("nodes %d\n", Ident::next);
  return 0;
}
