// n-cpp-concepts R13: a class template's redeclaration has the same constraints ([temp.over.link]/7)
template<typename T> concept Class = __is_class(T);
template<typename T> concept Union = __is_union(T);
template<typename T> requires Class<T> struct S1;
template<Class T>                  // { dg-error "different constraints" }
struct S1 { };
template<typename T> requires Class<T> struct S2;
template<typename T> requires Union<T>
struct S2;                         // { dg-error "different constraints" }
template<typename T> requires Class<T> struct S3;
template<typename U> requires Class<U> struct S3 { };
