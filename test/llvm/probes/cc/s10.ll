; verdict: invalid Function takes metadata but isn't an intrinsic
; rule: llvm/lib/IR/Verifier.cpp:3152 metadata parameter on a non-intrinsic definition (parser accepts metadata as an argument type).
define void @f(metadata %a) {
 ret void
}
