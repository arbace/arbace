; verdict: invalid cannot guarantee tail call due to mismatched ABI impacting function attributes
; rule: llvm/lib/IR/Verifier.cpp:4233 Call argument has inreg, caller parameter does not.
declare void @g(i32 inreg)
define void @f(i32 %x) {
 musttail call void @g(i32 inreg %x)
 ret void
}
