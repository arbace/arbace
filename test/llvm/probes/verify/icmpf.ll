; verdict: invalid icmp requires integer operands
; rule: llvm/lib/AsmParser/LLParser.cpp:8549 icmp on float fails in parseCompare.
define i1 @f(float %c) {
  %x = icmp eq float %c, %c
  ret i1 %x
}
