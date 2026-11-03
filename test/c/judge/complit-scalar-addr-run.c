/* queue 178 (CX8's finding 1; fixed by queue 173's place of a compound literal): the address of a scalar compound
   literal, automatic and static, is its object's, not its value (memcpy from &(unsigned){7u} faulted at 0x7).
   Self-checking: status 0 when ci agrees with gcc 15.2, 1..N the first check that failed. */
#include <string.h>
static unsigned *counter(void) { return &(static unsigned){40}; }
int main(void) {
  unsigned x = 0;
  memcpy(&x, &(unsigned){7u}, 4);
  if (x != 7) return 1;
  unsigned long long w = 0;
  memcpy(&w, &(unsigned long long){0x123456789ull}, sizeof w);
  if (w != 0x123456789ull) return 2;
  double d = 0;
  memcpy(&d, &(double){2.5}, sizeof d);
  if (d != 2.5) return 3;
  short *sp = &(short){-3};
  *sp += 1;
  if (*sp != -2) return 4;
  ++*counter();
  ++*counter();
  if (*counter() != 42) return 5;
  unsigned char b[3];
  memcpy(b, &(unsigned){0x00030201u}, 3);
  if (b[0] + b[1] + b[2] != 6) return 6;
  return 0;
}
