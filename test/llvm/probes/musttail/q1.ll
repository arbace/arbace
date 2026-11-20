; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4165 Caller f(i32) and callee g(i32) match in varargs, return, cc, params; the call is followed by ret void.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x)
 ret void
}
