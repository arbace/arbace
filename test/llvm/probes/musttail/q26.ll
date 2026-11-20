; verdict: invalid Expected exactly one cfguardtarget bundle operand
; rule: llvm/lib/IR/Verifier.cpp:4062 cfguardtarget bundle with zero inputs.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "cfguardtarget"() ]
 ret void
}
