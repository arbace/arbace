/* Variably modified types at run time (queue 165): sizeof of a VLA type name and of an expression through a pointer
   to a VLA, pointer arithmetic over VLA rows, a parameter's array size evaluated at entry with its effects, a
   parameter of pointer-to-VLA type, an unnamed parameter's size, typeof and auto of a variably modified type, and a
   VLA's empty initializer. Arbace's own, under the EPL-1.0 (LICENSE.md). */
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
  printf ("typedef %ld %ld\n", typedef_size (3), typedef_size (20));
  printf ("rows %ld\n", rows (4, 3));
  printf ("sides %d\n", sides (1, e, 4, e));
  printf ("unnamed %d\n", unnamed (1, e, 3));
  int r = rowparam (5, (char (*)[5]) buf);
  printf ("rowparam %d %d\n", r, buf[5]);
  printf ("types %ld\n", types (2));
  printf ("zeroed %d %d\n", zeroed (50), zeroed (50));
  return 0;
}
