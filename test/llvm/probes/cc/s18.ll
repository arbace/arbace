; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3069 naked function with an unused argument passes.
define void @f(ptr %x) naked {
 ret void
}
