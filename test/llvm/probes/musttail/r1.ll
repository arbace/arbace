; verdict: invalid cannot guarantee tail call due to mismatched ABI impacting function attributes
; rule: llvm/lib/IR/Verifier.cpp:4233 Call site has sret(i32) on the argument but the caller's parameter does not.
declare void @g(ptr sret(i32))
define void @f(ptr %x) {
 musttail call void @g(ptr sret(i32) %x)
 ret void
}
