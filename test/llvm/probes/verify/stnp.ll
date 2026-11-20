; verdict: invalid store operand must be a pointer
; rule: llvm/lib/AsmParser/LLParser.cpp:9044 store i32 1, i32 %p.
define void @f(i32 %p) {
  store i32 1, i32 %p
  ret void
}
