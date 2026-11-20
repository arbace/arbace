; verdict: invalid inalloca isn't on the last parameter!
; rule: llvm/lib/IR/Verifier.cpp:2343 Call-site attributes go through verifyFunctionAttrs: inalloca on param 0 of 2.
declare void @g(ptr, i32)
define void @f(ptr %p) {
 call void @g(ptr inalloca(i32) %p, i32 1)
 ret void
}
