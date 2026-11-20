; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4143 zeroext is not in the ABI attribute list of getParameterABIAttributes, so the caller/callee sets are both empty.
declare void @g(i32 zeroext)
define void @f(i32 %x) {
 musttail call void @g(i32 zeroext %x)
 ret void
}
