; verdict: invalid Multiple deopt operand bundles
; rule: llvm/lib/IR/Verifier.cpp:4043 Two deopt bundles on one call.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "deopt"(), "deopt"() ]
 ret void
}
