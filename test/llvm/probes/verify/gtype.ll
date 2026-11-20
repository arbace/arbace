; verdict: invalid global variable reference must have pointer type
; rule: llvm/lib/AsmParser/LLParser.cpp:1874 ret i32 @f: @f used as an i32 value; getGlobalVal requires a pointer type.
define i32 @f() {
  ret i32 @f
}
