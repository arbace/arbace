// queue 423: a move-only class (its copy assignment implicitly deleted, [class.copy.assign]/7 with a user-declared move
// constructor): T{} and a braced list assign by the move assignment; an lvalue's assignment is the deleted copy's
struct M { M() {} M(M&&) {} M& operator=(M&&) { return *this; } int v; };
template<class T> void g(T& t) { t = T{}; t = {}; }
template<class T> void c(T& t, T& u) { t = u; }   // { dg-error "deleted" }
void h() { M m, n; g(m); m = M{}; m = {}; c(m, n); }
