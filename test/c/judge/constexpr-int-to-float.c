/* { dg-do compile } */
/* { dg-options "-std=c23" } */
/* Review 057's finding 4: an integer is rounded once to float (2^53 + 2^29 + 1 is above the tie of the double 2^53 + 2^29, so it rounds up). */
_Static_assert((unsigned long long)(float)9007199791611905ULL == 9007200328482816ULL);
_Static_assert((int)(float)16777217 == 16777216);
_Static_assert((int)(float)16777219 == 16777220);
