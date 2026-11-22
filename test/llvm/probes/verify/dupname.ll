; verdict: invalid multiple definition of local value named 'x'
; rule: llvm/lib/AsmParser/LLParser.cpp:4113 Second %x: setName auto-renames to x1, name differs so setInstName errors.
define i32 @f() {
  %x = add i32 1, 2
  %x = add i32 1, 2
  ret i32 %x
}
