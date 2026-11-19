// queue 423: both assignments user-declared: a prvalue and an empty braced list take operator=(B&&) ([over.ics.rank]/3.2.3,
// the list's temporary an rvalue, [over.ics.list]/9); a list of one B lvalue binds the copy's reference to it
// ([dcl.init.list]/3.10), the move's being no candidate for it
struct B { B() {} B(const B&) {} B(B&&) {} B& operator=(const B&) { return *this; } B& operator=(B&&) { return *this; } };
template<class T> void g(T& t, T& u) { t = T{}; t = {}; t = {u}; t = u; }
void h() { B b, c; g(b, c); b = B{}; b = {}; b = {c}; b = c; }
