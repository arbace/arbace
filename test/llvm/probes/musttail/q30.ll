; verdict: invalid The 'convergencectrl' bundle requires exactly one token use.
; rule: llvm/lib/IR/ConvergenceVerifier.cpp:56 ConvergenceVerifyHelper.visit(Call) finds a convergencectrl bundle with zero inputs; visitCallBase has no earlier failing check.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "convergencectrl"() ]
 ret void
}
