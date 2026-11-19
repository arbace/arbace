// queue 423: a class whose copy assignment is deleted and move assignment defaulted: a prvalue and a braced list take the
// move assignment; an lvalue the deleted copy assignment ([dcl.fct.def.delete]/2)
struct D { D() = default; D& operator=(const D&) = delete; D& operator=(D&&) = default; int v; };
template<class T> void g(T& t) { t = T{}; t = {}; }
void h() { D d, e; g(d); d = D{}; d = {};
  d = e;   // { dg-error "deleted" }
}
