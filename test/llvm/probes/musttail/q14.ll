; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4143 zeroext on both sides and not ABI impacting.
declare void @g(i32 zeroext)
define void @f(i32 zeroext %x) {
 musttail call void @g(i32 zeroext %x)
 ret void
}
