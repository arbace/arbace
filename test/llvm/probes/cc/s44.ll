; verdict: invalid Attribute 'sret' cannot be used for vararg call arguments!
; rule: llvm/lib/IR/Verifier.cpp:4000 sret on an argument in the varargs part of the call.
declare void @g(ptr, ...)
define void @f(ptr %p) {
 call void (ptr, ...) @g(ptr %p, ptr sret(i32) %p)
 ret void
}
