; verdict: invalid load operand must be a pointer to a first class type
; rule: llvm/lib/AsmParser/LLParser.cpp:8996 load i32, i32 %p: operand is not a pointer.
define i32 @f(i32 %p) {
  %v = load i32, i32 %p
  ret i32 %v
}
