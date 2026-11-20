; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:377 Undeclared llvm.memcpy.p0.p0.i64 used only as callee with a valid signature: validateEndOfModule inserts the intrinsic declaration automatically.
define void @f(ptr %a) {
  call void @llvm.memcpy.p0.p0.i64(ptr %a, ptr %a, i64 1, i1 false)
  ret void
}
