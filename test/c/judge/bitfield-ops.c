/* { dg-do compile } */
/* { dg-options "-std=gnu23" } */
/* Review 027's finding 7: an assignment or an increment of a bit-field is no bit-field (gcc 15 accepts these). */
struct s { int bf : 3; unsigned u : 5; } s, *p = &s;
unsigned long a = sizeof (s.bf = 1);
unsigned long b = sizeof (s.bf++);
unsigned long c = sizeof (++s.bf);
unsigned long d = sizeof (s.bf += 2);
unsigned long e = sizeof (p->u--);
unsigned long f = _Alignof (s.bf = 1);
typeof (s.bf = 1) g;
typeof (s.bf++) h;
typeof_unqual (--s.bf) i;
/* the bit-field itself */
unsigned long j = sizeof (s.bf);		/* { dg-error "'sizeof' applied to a bit-field" } */
unsigned long k = sizeof ((p->u));		/* { dg-error "'sizeof' applied to a bit-field" } */
typeof (s.bf) l;				/* { dg-error "'typeof' applied to a bit-field" } */
unsigned long m = _Alignof (p->bf);		/* { dg-error "applied to a bit-field" } */
