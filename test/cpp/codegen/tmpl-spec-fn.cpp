// explicit specializations of a function template: with and without the arguments written
#include <cstdio>
template <class T> const char* name(T) { return "other"; }
template <> const char* name<int>(int) { return "int"; }
template <> const char* name(double) { return "double"; }
template <class T> int size_of() { return (int)sizeof(T); }
template <> int size_of<void>() { return 0; }
int main() {
  printf("%s %s %s %s\n", name(1), name(1.0), name('c'), name(2L));
  printf("%d %d %d\n", size_of<int>(), size_of<void>(), size_of<long>());
  return 0;
}
