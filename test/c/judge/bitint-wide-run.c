/* queue 164: _BitInt wider than 64 bits (and __int128) where ci's builder refused them or built an integer node for
   them: bit-fields of wide types (load, store, initializer, compound assignment, ++), postfix ++ and -- of a wide
   object, a wide statement expression, a wide switch (its cases and a GNU case range), and gcc's promotion of a
   bit-field of a 128-bit type (an N-bit type: its arithmetic wraps at the width) and of a _BitInt type (its
   declared type).
   Self-checking: status 0 when ci agrees with gcc 15.2 (x86-64), 1..N the first check that failed. */

struct S {
  int i : 3;
  _BitInt(200) a : 135;
  unsigned _BitInt(200) u : 100;
  __int128 l : 120;
  unsigned __int128 o : 125;
  unsigned m : 5;
  _BitInt(511) b : 300;
};

static int fails;
#define CHECK(n, c) do { if (!(c) && !fails) fails = (n); } while (0)

__attribute__((noipa)) _BitInt(200) geta (struct S *p) { return p->a; }

__attribute__((noipa)) int
sel (_BitInt(150) x)
{
  switch (x)
    {
    case -1180591620717411303424wb: return 1;                       /* -(2^70) */
    case 1393796574908163946345982392040522594123776wb: return 2;     /* 2^140 */
    case 5wb ... 9wb: return 3;
    case 0: return 4;
    default: return 5;
    }
}

int
main ()
{
  struct S s[2] = { { -2, -13055525270329736316393717310914023773847wb, 1, -1, 7, 9, 3 },
                    { 1, 1, 1, 1, 1, 1, 1 } };
  CHECK (1, s[0].i == -2 && s[0].m == 9 && s[1].i == 1 && s[1].m == 1);
  CHECK (2, geta (&s[0]) == -13055525270329736316393717310914023773847wb);
  CHECK (3, s[0].u == 1 && s[0].l == -1 && s[0].o == 7 && s[0].b == 3);
  CHECK (4, geta (&s[1]) == 1 && s[1].u == 1 && s[1].l == 1 && s[1].o == 1 && s[1].b == 1);

  /* stores keep the neighbours */
  s[0].a = 21778071482940061661655974875633165533183wb;   /* 2^134 - 1 */
  CHECK (5, s[0].a == 21778071482940061661655974875633165533183wb && s[0].i == -2 && s[0].u == 1);
  s[0].a = -21778071482940061661655974875633165533184wb;  /* -(2^134) */
  CHECK (6, s[0].a == -21778071482940061661655974875633165533184wb && s[0].u == 1);
  /* a store's value is the field's: truncated to 135 bits, sign-extended */
  _BitInt(200) v = (s[1].a = 21778071482940061661655974875633165533184wb); /* 2^134 wraps to -(2^134) */
  CHECK (7, v == -21778071482940061661655974875633165533184wb && s[1].a == v);
  s[0].u = 0;
  s[0].u -= 1;                                           /* 2^100 - 1 */
  CHECK (8, s[0].u == 1267650600228229401496703205375uwb);
  /* gcc: a _BitInt bit-field is promoted to its declared type (c-typeck.cc's perform_integral_promotions), no wrap */
  CHECK (9, s[0].u + 1 == 1267650600228229401496703205376uwb);
  CHECK (10, sizeof (s[0].u + 1) == 32);
  s[0].u++;
  CHECK (11, s[0].u == 0 && s[0].l == -1);
  s[0].l = 664613997892457936451903530140172287wb;       /* 2^119 - 1 */
  CHECK (12, s[0].l == 664613997892457936451903530140172287wb);
  /* gcc: an unsigned 128-bit type's bit-field is an N-bit type, so o + 1 wraps */
  s[1].o = ~(unsigned __int128) 0;
  CHECK (13, s[1].o + 1 == 0 && s[1].m == 1);
  __int128 old = s[0].l++;
  CHECK (14, old == 664613997892457936451903530140172287wb && s[0].l == -664613997892457936451903530140172288wb);
  s[0].o = ~(unsigned __int128) 0;
  CHECK (15, s[0].o == (~(unsigned __int128) 0 >> 3) && s[0].m == 9 && s[0].b == 3);
  s[0].b = -2655156766298562299560755420298083843774074962786295887660222690220887wb;
  CHECK (16, (s[0].b << 1) == ((_BitInt(511)) -2655156766298562299560755420298083843774074962786295887660222690220887wb) << 1);
  CHECK (17, s[0].o == (~(unsigned __int128) 0 >> 3) && s[0].m == 9);

  /* postfix ++ and -- of a wide object */
  _BitInt(300) w = 18446744073709551615wb;               /* 2^64 - 1 */
  _BitInt(300) w0 = w++;
  CHECK (18, w0 == 18446744073709551615wb && w == 18446744073709551616wb);
  w0 = w--;
  CHECK (19, w0 == 18446744073709551616wb && w == 18446744073709551615wb);
  unsigned __int128 q = 0;
  unsigned __int128 q0 = q--;
  CHECK (20, q0 == 0 && q == ~(unsigned __int128) 0);

  /* a wide statement expression */
  _BitInt(250) e = ({ _BitInt(250) t = w; t * t; });
  CHECK (21, e == 340282366920938463426481119284349108225wb);   /* (2^64 - 1)^2 */

  /* a wide switch */
  CHECK (22, sel (-1180591620717411303424wb) == 1);
  CHECK (23, sel (1393796574908163946345982392040522594123776wb) == 2);
  CHECK (24, sel (7) == 3 && sel (5) == 3 && sel (9) == 3 && sel (10) == 5);
  CHECK (25, sel (0) == 4 && sel (-5) == 5);
  CHECK (26, sel (1180591620717411303424wb) == 5);
  return fails;
}
