/* bin/hsi libm-check's reference (HE4b, doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE4b, HE4c (write-ahead)"):
   built by the host's cc against the host's libm (musl 1.2.6, the libm GHC's programs call), it writes the lines
   NAME A B R (hexadecimal bit patterns) for each function hsi ports, over inputs chosen deterministically: the
   edges (zeros, subnormals, the normal bounds, infinities, NaNs, the functions' thresholds, multiples of pi/2),
   random bit patterns over the whole range and random values in ranges of several widths (a xorshift generator
   seeded by its argument). `hsi libm` recomputes every line with hsi's port and counts the mismatches.
   usage: libm-ref [N [SEED]], N random inputs of each kind per function (default 20000). */
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static uint64_t st = 88172645463325252ull;
static uint64_t rnd(void) { st ^= st << 13; st ^= st >> 7; st ^= st << 17; return st; }
static double ud(double lo, double hi) { return lo + (hi - lo) * ((rnd() >> 11) * 0x1p-53); }
static uint64_t b64(double x) { uint64_t u; memcpy(&u, &x, 8); return u; }
static double d64(uint64_t u) { double x; memcpy(&x, &u, 8); return x; }
static uint32_t b32(float x) { uint32_t u; memcpy(&u, &x, 4); return u; }
static float f32(uint32_t u) { float x; memcpy(&x, &u, 4); return x; }

typedef double (*fd)(double);
typedef float (*ff)(float);
static const struct { const char *n; fd f; } D1[] = {
	{"exp", exp}, {"expm1", expm1}, {"log", log}, {"log1p", log1p}, {"sin", sin}, {"cos", cos}, {"tan", tan},
	{"asin", asin}, {"acos", acos}, {"atan", atan}, {"sinh", sinh}, {"cosh", cosh}, {"tanh", tanh},
	{"asinh", asinh}, {"acosh", acosh}, {"atanh", atanh}};
static const struct { const char *n; ff f; } F1[] = {
	{"expf", expf}, {"expm1f", expm1f}, {"logf", logf}, {"log1pf", log1pf}, {"sinf", sinf}, {"cosf", cosf},
	{"tanf", tanf}, {"asinf", asinf}, {"acosf", acosf}, {"atanf", atanf}, {"sinhf", sinhf}, {"coshf", coshf},
	{"tanhf", tanhf}, {"asinhf", asinhf}, {"acoshf", acoshf}, {"atanhf", atanhf}};

/* the double edges: each also negated */
static const double ED[] = {0.0, 0x1p-1074, 0x1p-1060, 0x1.fffffffffffffp-1023, 0x1p-1022, 0x1p-1000, 0x1p-500,
	0x1p-60, 0x1p-54, 0x1p-53, 0x1p-28, 0x1p-27, 0x1p-26, 1e-10, 1e-5, 0.1, 0.25, 0.2554, 0.5, 0.5493, 0.6744,
	0.75, 0.975, 0x1.fffffffffffffp-1, 1.0, 0x1.0000000000001p0, 1.125, 1.5, 2.0, 2.4375, 3.0, 10.0, 20.0, 22.0,
	56 * 0.6931471805599453, 100.0, 512.0, 709.0, 709.782712893384, 709.79, 710.0, 744.0, 745.13321910194122,
	745.2, 1000.0, 1024.0, 1075.0, 1e10, 0x1p26, 0x1p27, 0x1p52, 0x1p53, 1e22, 0x1p63, 0x1p64, 0x1p66, 1e100,
	1e200, 1e300, 1e308, 0x1.fffffffffffffp1023, 1.5707963267948966, 3.141592653589793, 4.71238898038469,
	6.283185307179586, 7.853981633974483, 10.995574287564276, 12.566370614359172, 1.5707963267948966e5,
	1.5707963267948966e6, 1.5707963267948966e10, 1.5707963267948966e20, 0x1.921fb54442d18p20, 0x1.921fb54442d18p30,
	INFINITY};
static const float EF[] = {0.0f, 0x1p-149f, 0x1p-140f, 0x1.fffffcp-127f, 0x1p-126f, 0x1p-100f, 0x1p-30f, 0x1p-25f,
	0x1p-24f, 0x1p-13f, 0x1p-12f, 0x1p-11f, 1e-5f, 0.1f, 0.25f, 0.2554f, 0.5f, 0.5493f, 0.75f, 0x1.fffffep-1f, 1.0f,
	0x1.000002p0f, 1.125f, 1.5f, 2.0f, 2.4375f, 3.0f, 10.0f, 18.7f, 27 * 0.6931472f, 50.0f, 88.0f, 88.72f, 88.73f,
	89.0f, 103.9f, 104.0f, 128.0f, 150.0f, 1000.0f, 4096.0f, 0x1p26f, 1e10f, 1e20f, 1e30f, 0x1.fffffep127f,
	1.5707964f, 2.3561945f, 3.1415927f, 3.926991f, 4.712389f, 5.497787f, 6.2831855f, 7.0685835f, 1.5707964e5f,
	1.5707964e8f, 0x1.921fb6p28f, 1e38f, INFINITY};

static void d1(const char *n, fd f, double x) { printf("%s %llx 0 %llx\n", n, (unsigned long long)b64(x), (unsigned long long)b64(f(x))); }
static void f1(const char *n, ff f, float x) { printf("%s %x 0 %x\n", n, b32(x), b32(f(x))); }
static void d2(const char *n, double (*f)(double, double), double x, double y) {
	printf("%s %llx %llx %llx\n", n, (unsigned long long)b64(x), (unsigned long long)b64(y), (unsigned long long)b64(f(x, y)));
}
static void f2(const char *n, float (*f)(float, float), float x, float y) {
	printf("%s %x %x %x\n", n, b32(x), b32(y), b32(f(x, y)));
}

int main(int argc, char **argv) {
	int n = argc > 1 ? atoi(argv[1]) : 20000;
	if (argc > 2) st = strtoull(argv[2], 0, 10) | 1;
	static const double R[] = {1.0, 4.0, 40.0, 800.0, 1e6};
	static const float RF[] = {1.0f, 4.0f, 40.0f, 120.0f, 1e5f};
	double nand = d64(0x7ff8000000000000ull), nann = d64(0xfff8000000000000ull), nanp = d64(0x7ff4000000000123ull | 0x8000000000000ull);
	float nanf_ = f32(0x7fc00000u), nanfn = f32(0xffc00000u);
	for (size_t i = 0; i < sizeof D1 / sizeof *D1; i++) {
		for (size_t e = 0; e < sizeof ED / sizeof *ED; e++) {
			d1(D1[i].n, D1[i].f, ED[e]); d1(D1[i].n, D1[i].f, -ED[e]);
			d1(D1[i].n, D1[i].f, d64(b64(ED[e]) + 1)); d1(D1[i].n, D1[i].f, d64(b64(ED[e]) - 1));
		}
		d1(D1[i].n, D1[i].f, nand); d1(D1[i].n, D1[i].f, nann); d1(D1[i].n, D1[i].f, nanp);
		for (int k = 0; k < n; k++) d1(D1[i].n, D1[i].f, d64(rnd()));
		for (size_t r = 0; r < sizeof R / sizeof *R; r++)
			for (int k = 0; k < n / 5; k++) d1(D1[i].n, D1[i].f, ud(-R[r], R[r]));
		for (int k = 0; k < n / 5; k++) d1(D1[i].n, D1[i].f, d64((uint64_t)(rnd() % 0x10000000000000ull)));
	}
	for (size_t i = 0; i < sizeof F1 / sizeof *F1; i++) {
		for (size_t e = 0; e < sizeof EF / sizeof *EF; e++) {
			f1(F1[i].n, F1[i].f, EF[e]); f1(F1[i].n, F1[i].f, -EF[e]);
			f1(F1[i].n, F1[i].f, f32(b32(EF[e]) + 1)); f1(F1[i].n, F1[i].f, f32(b32(EF[e]) - 1));
		}
		f1(F1[i].n, F1[i].f, nanf_); f1(F1[i].n, F1[i].f, nanfn);
		for (int k = 0; k < n; k++) f1(F1[i].n, F1[i].f, f32((uint32_t)rnd()));
		for (size_t r = 0; r < sizeof RF / sizeof *RF; r++)
			for (int k = 0; k < n / 5; k++) f1(F1[i].n, F1[i].f, (float)ud(-RF[r], RF[r]));
		for (int k = 0; k < n / 5; k++) f1(F1[i].n, F1[i].f, f32((uint32_t)(rnd() % 0x800000u)));
	}
	/* pow, atan2 and hypot: the edges' cross product, random bit patterns, and values in ranges: x in (0, 10) with y in
	   (-60, 60), x negative with an integer y, x near 1 with large y, subnormal x */
	for (size_t a = 0; a < sizeof ED / sizeof *ED; a++)
		for (size_t b = 0; b < sizeof ED / sizeof *ED; b++)
			for (int s = 0; s < 4; s++) {
				double x = s & 1 ? -ED[a] : ED[a], y = s & 2 ? -ED[b] : ED[b];
				d2("pow", pow, x, y); d2("atan2", atan2, x, y); d2("hypot", hypot, x, y);
			}
	for (size_t a = 0; a < sizeof EF / sizeof *EF; a++)
		for (size_t b = 0; b < sizeof EF / sizeof *EF; b++)
			for (int s = 0; s < 4; s++) {
				float x = s & 1 ? -EF[a] : EF[a], y = s & 2 ? -EF[b] : EF[b];
				f2("powf", powf, x, y); f2("atan2f", atan2f, x, y);
			}
	double nans[] = {nand, nann, nanp, 1.0, -1.0, 0.0, INFINITY};
	for (int a = 0; a < 7; a++) for (int b = 0; b < 7; b++) {
		d2("pow", pow, nans[a], nans[b]); d2("atan2", atan2, nans[a], nans[b]); d2("hypot", hypot, nans[a], nans[b]);
	}
	for (int k = 0; k < n; k++) {
		d2("pow", pow, d64(rnd()), d64(rnd())); d2("atan2", atan2, d64(rnd()), d64(rnd()));
		d2("pow", pow, ud(0, 10), ud(-60, 60)); d2("atan2", atan2, ud(-10, 10), ud(-10, 10));
		d2("pow", pow, -ud(0, 10), (double)(int)ud(-40, 40)); d2("pow", pow, ud(0.999, 1.001), ud(-1e6, 1e6));
		d2("pow", pow, d64(rnd() % 0x10000000000000ull), ud(-2, 2));
		/* hypot: bit patterns, ordinary magnitudes, close ones, and the scaled ranges near overflow and underflow */
		d2("hypot", hypot, d64(rnd()), d64(rnd())); d2("hypot", hypot, ud(-10, 10), ud(-10, 10));
		{ double h = ud(-1e3, 1e3); d2("hypot", hypot, h, h * (1 + ud(-1e-8, 1e-8))); }
		d2("hypot", hypot, ud(1e300, 1e308), ud(1e300, 1e308)); d2("hypot", hypot, ud(1e-310, 1e-300), ud(1e-310, 1e-300));
		f2("powf", powf, f32((uint32_t)rnd()), f32((uint32_t)rnd())); f2("atan2f", atan2f, f32((uint32_t)rnd()), f32((uint32_t)rnd()));
		f2("powf", powf, (float)ud(0, 10), (float)ud(-40, 40)); f2("atan2f", atan2f, (float)ud(-10, 10), (float)ud(-10, 10));
		f2("powf", powf, -(float)ud(0, 10), (float)(int)ud(-30, 30)); f2("powf", powf, (float)ud(0.99, 1.01), (float)ud(-1e4, 1e4));
		f2("powf", powf, f32((uint32_t)(rnd() % 0x800000u)), (float)ud(-2, 2));
	}
	(void)nanf_; (void)nanfn;
	return 0;
}
