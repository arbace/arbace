; verdict: invalid use of undefined value '@nosuch'
; rule: llvm/lib/AsmParser/LLParser.cpp:440 Call to a never-declared non-intrinsic function stays in ForwardRefVals at end of module.
define void @f() {
  call void @nosuch()
  ret void
}
