#include <vector>
template<class T> struct W { T v; T get() const { return v; } };
template<class T> concept Num = requires(T t){ t+t; };
template<Num T> T twice(T x){ return x+x; }
struct S { S(){} ~S(){} };
int f(){ S s; auto l=[&](int a){ return twice(a)+W<int>{1}.get(); }; try { throw 1; } catch(int){ } return l(2); }
int main(){ std::vector<int> v{1,2}; return f()+v.size(); }
