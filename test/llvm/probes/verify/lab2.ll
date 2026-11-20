; verdict: invalid label expected to be numbered '1' or greater
; rule: llvm/lib/AsmParser/LLParser.cpp:3598 Entry block is implicitly %0; the later label 0: fails checkValueID (called from defineBB at 4139).
define void @f() {
  br label %0
0:
  ret void
}
