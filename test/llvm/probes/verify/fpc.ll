; verdict: invalid floating-point constant invalid for type
; rule: llvm/lib/AsmParser/LLParser.cpp:4218 `ret i32 1.5`: the decimal 1.5 lexes as a FloatLiteral (LLLexer.cpp:1308), parsed with the expected type i32, not floating point (corrected after the oracle comparison: the first review took the APFloat token's error at LLParser.cpp:6873)
define i32 @f() {
  ret i32 1.5
}
