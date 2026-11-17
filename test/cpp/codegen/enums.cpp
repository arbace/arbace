// enumerations: unscoped and scoped, fixed underlying types, switches over them, conversions
#include <cstdio>
enum Color { Red, Green = 5, Blue };
enum class Dir : unsigned char { North = 1, East, South, West };
const char *name(Dir d) {
  switch (d) {
  case Dir::North: return "N";
  case Dir::East: return "E";
  case Dir::South: return "S";
  default: return "W";
  }
}
Dir turn(Dir d) { return d == Dir::West ? Dir::North : static_cast<Dir>(static_cast<int>(d) + 1); }
int main() {
  Color c = Blue;
  int ci = c + 1;
  printf("%d %d %d\n", Red, c, ci);
  Dir d = Dir::South;
  for (int i = 0; i < 5; i++) { printf("%s ", name(d)); d = turn(d); }
  printf("\n%d %d\n", (int)sizeof(Dir), (int)sizeof(Color));
  bool lt = Green < Blue;
  printf("%d\n", lt);
  return static_cast<int>(Dir::East);
}
