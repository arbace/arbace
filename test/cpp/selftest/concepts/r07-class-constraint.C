// n-cpp-concepts R7: a constrained class template's simple-template-id satisfies its constraints ([temp.names]/7)
template<typename T> concept Class = __is_class(T);
template<typename T> requires true struct ok { };
template<typename T> requires false struct err { };
template<Class T> struct box { };
struct E { };
ok<int> ok1;
err<int> err1;   // { dg-error "constraint" }
box<E> b1;
box<int> b2;     // { dg-error "constraint" }
