// D342-3 (a): a template template parameter's own default (`template<class = int> class TT`) is not the concept's: Q2 takes 2
template<class T, template<class = int> class TT> concept Q2 = true;
template<Q2 T> struct S { };   // { dg-error "wrong number of template arguments .1, should be 2." }
template<class T, int N = 3> concept I = true;
template<class T, template<class = int> class TT = T::template tt> concept J = true;
template<I T> struct G { };
template<J T> struct H { };
template<I<4, 5> T> struct K { };  // { dg-error "wrong number of template arguments .3, should be at least 1." }
