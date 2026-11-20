; verdict: invalid void type only allowed for function results
; rule: llvm/lib/AsmParser/LLParser.cpp:3271 void parameter type: parseType with AllowVoid false.
define void @f(void %x) {
 ret void
}
