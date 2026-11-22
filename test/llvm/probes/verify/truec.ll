; verdict: invalid constant expression type mismatch: got type 'i1' but expected 'i32'
; rule: llvm/lib/AsmParser/LLParser.cpp:6944 true is an i1 constant; ret i32 true mismatches in convertValIDToValue.
define i32 @f() {
  ret i32 true
}
