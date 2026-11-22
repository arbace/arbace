; verdict: invalid Return type cannot be token for indirect call!
; rule: llvm/lib/IR/Verifier.cpp:4022 Indirect call (no called function) returning token.
define void @f(ptr %x) {
 %t = call token %x()
 ret void
}
