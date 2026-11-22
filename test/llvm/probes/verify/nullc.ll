; verdict: invalid null must be a pointer type
; rule: llvm/lib/AsmParser/LLParser.cpp:6908 ret i32 null.
define i32 @f() {
  ret i32 null
}
