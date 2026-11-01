/* { dg-do compile } */
/* { dg-options "-std=c23" } */
/* Review 079's finding 6: equal-rank floating types: _FloatN over the standard type over _FloatNx (gcc 15). */
#define T(a,b,k) _Static_assert(_Generic((a)+(b), k: 1, default: 0), #a "+" #b)
T(1.0f,(_Float32)1,_Float32);
T((_Float32)1,1.0f,_Float32);
T(1.0,(_Float64)1,_Float64);
T((_Float64)1,1.0,_Float64);
T((_Float32x)1,1.0,double);
T(1.0,(_Float32x)1,double);
T((_Float32x)1,(_Float64)1,_Float64);
T((_Float64)1,(_Float32x)1,_Float64);
T((_Float32x)1,(_Float32)1,_Float32x);
T(1.0L,(_Float64x)1,long double);
T((_Float64x)1,1.0L,long double);
T((_Float128)1,(_Float64x)1,_Float128);
T(1.0f,(_Float32x)1,_Float32x);
