// 6.7.3.3: an enumeration with a fixed underlying type declared before it is
// defined.
enum e : long;
enum e : long { A, B, };
