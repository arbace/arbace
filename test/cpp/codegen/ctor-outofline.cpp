struct B { B (); virtual void f () = 0; };
B::B () {}
struct D : public B { virtual void f () {} };
int main () { D d; d.f(); }
