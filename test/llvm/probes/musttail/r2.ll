; verdict: invalid cannot guarantee tail call due to mismatched ABI impacting function attributes
; rule: llvm/lib/IR/Verifier.cpp:4233 Caller parameter has sret(i32), the call argument has none.
declare void @g(ptr)
define void @f(ptr sret(i32) %x) {
 musttail call void @g(ptr %x)
 ret void
}
