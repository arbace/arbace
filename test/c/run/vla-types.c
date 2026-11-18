/* ci-run verdict: exit 0. Variably modified types at run time (queue 165): sizeof of a VLA type name and of an expression through a pointer
   to a VLA, pointer arithmetic over VLA rows, a parameter's array size evaluated at entry with its effects, a
   parameter of pointer-to-VLA type, an unnamed parameter's size, typeof and auto of a variably modified type, and a
   VLA's empty initializer. Checks itself: each value worked out by hand (typedef 4 (3 + 2), 4 (20 + 2); rows 48 + 12 + 12
   + 24 + 2 + 21 + 12 + 1; sides 2 * 10 + 5; unnamed 2 * 10 + 3; rowparam 5, 7; types (n 3) 3 + 12, + 12 + 1 call, + 16 + 2
   calls, + 8 + 3, the operands of + taken left to right and the pointer type's size expression not evaluated, the two
   choices C leaves unspecified that this program states; zeroed 0); a difference returns 1. Arbace's own, under the
   EPL-1.0 (LICENSE.md). */
int printf (const char *, ...);

static int calls;
static int count (int n) { calls++; return n; }

static long typedef_size (int i)
{
  typedef int c[i + 2];
  i = 100; /* the typedef's size is fixed where it stands */
  return sizeof (c);
}

static long rows (int n, int m)
{
  int v[n][m];
  int (*p)[m] = v;
  long s = sizeof v + sizeof v[1] + sizeof (*p);
  p++;
  p += 1;
  s += (char *) p - (char *) v;
  s += p - &v[0];
  for (int i = 0; i < n; i++)
    for (int j = 0; j < m; j++)
      v[i][j] = i * 10 + j;
  s += v[2][1] + p[-1][2] + (*(p - 2))[1];
  return s;
}

static int sides (int a, int b[a++], int c, int d[c++])
{
  return a * 10 + c;
}

static int unnamed (int a, int [++a], int b)
{
  return a * 10 + b;
}

static int rowparam (int n, char x[2][n])
{
  x[1][0] = 7;
  return x[1] - x[0];
}

static long types (int n)
{
  typeof (int (*)[++n]) p = 0;
  long s = n + sizeof (*p);
  auto q = (int (*)[count (n)]) 0;
  s += sizeof (*q) + calls;
  s += sizeof (int[count (n + 1)]) + calls;
  s += sizeof (typeof (int (*)[++n])) + n; /* a pointer type: its size expression not evaluated */
  return s;
}

static int zeroed (int n)
{
  volatile int vla[n] = {};
  int bad = 0;
  for (int i = 0; i < n; i++)
    bad += vla[i] != 0;
  for (int i = 0; i < n; i++)
    vla[i] = -1;
  return bad;
}

int main (void)
{
  int e[10];
  char buf[64];
  long t1 = typedef_size (3), t2 = typedef_size (20);
  printf ("typedef %ld %ld\n", t1, t2);
  long rw = rows (4, 3);
  printf ("rows %ld\n", rw);
  int sd = sides (1, e, 4, e);
  printf ("sides %d\n", sd);
  int un = unnamed (1, e, 3);
  printf ("unnamed %d\n", un);
  int r = rowparam (5, (char (*)[5]) buf);
  printf ("rowparam %d %d\n", r, buf[5]);
  long ty = types (2);
  printf ("types %ld\n", ty);
  int z1 = zeroed (50), z2 = zeroed (50);
  printf ("zeroed %d %d\n", z1, z2);
  return t1 == 20 && t2 == 88 && rw == 132 && sd == 25 && un == 23 && r == 5 && buf[5] == 7 && ty == 57 && z1 == 0 && z2 == 0 ? 0 : 1;
}
