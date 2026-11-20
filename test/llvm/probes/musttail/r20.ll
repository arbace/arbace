; verdict: invalid cannot use musttail call with inline asm
; rule: llvm/lib/IR/Verifier.cpp:4166 The musttail call has an inline asm callee; verifyInlineAsmCall in visitCallBase passes for empty constraints.
define void @f(i32 %x) {
 musttail call void asm sideeffect "", ""()
 ret void
}
