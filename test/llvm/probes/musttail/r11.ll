; verdict: invalid cannot guarantee tail call due to mismatched ABI impacting function attributes
; rule: llvm/lib/IR/Verifier.cpp:4233 Call argument has inalloca(i32), caller parameter has none.
declare void @g(ptr inalloca(i32))
define void @f(ptr %x) {
 musttail call void @g(ptr inalloca(i32) %x)
 ret void
}
