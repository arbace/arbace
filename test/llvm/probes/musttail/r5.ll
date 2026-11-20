; verdict: invalid cannot guarantee tail call due to mismatched ABI impacting function attributes
; rule: llvm/lib/IR/Verifier.cpp:4233 byval(i64) on the caller vs byval(i32) on the call: the type attributes differ.
declare void @g(ptr byval(i32))
define void @f(ptr byval(i64) %x) {
 musttail call void @g(ptr byval(i32) %x)
 ret void
}
