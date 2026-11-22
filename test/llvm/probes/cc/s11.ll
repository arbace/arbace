; verdict: invalid Function takes token but isn't an intrinsic
; rule: llvm/lib/IR/Verifier.cpp:3154 token parameter on a non-intrinsic.
define void @f(token %a) {
 ret void
}
