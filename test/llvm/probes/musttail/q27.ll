; verdict: invalid Expected exactly two ptrauth bundle operands
; rule: llvm/lib/IR/Verifier.cpp:4067 ptrauth bundle has one input; the size check precedes the key type checks.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "ptrauth"(i32 0) ]
 ret void
}
