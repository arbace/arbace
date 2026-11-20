; verdict: invalid integer/byte constant must have integer/byte type
; rule: llvm/lib/AsmParser/LLParser.cpp:6865 ret ptr 1: t_APSInt with pointer type.
define ptr @f() {
  ret ptr 1
}
