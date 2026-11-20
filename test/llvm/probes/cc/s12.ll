; verdict: invalid Function returns a token but isn't an intrinsic
; rule: llvm/lib/IR/Verifier.cpp:3168 token return type on a non-intrinsic; checked in visitFunction before the body.
define token @f() {
 ret token none
}
