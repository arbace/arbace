// { dg-options "-std=c++98" }
#if true && !false && (1 and 2)
ok1 __cplusplus
#endif
#if __cplusplus >= 201103L
no
#endif
