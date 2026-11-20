; verdict: invalid invalid operand type for instruction
; rule: llvm/lib/AsmParser/LLParser.cpp:8503 parseArithmetic with IsFP=false requires an integer or integer-vector operand; add on float fails at parse.
define float @f(float %c) {
  %x = add float %c, %c
  ret float %x
}
