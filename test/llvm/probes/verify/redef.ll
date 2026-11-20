; verdict: invalid invalid redefinition of function 'f'
; rule: llvm/lib/AsmParser/LLParser.cpp:7219 Second define @f: M->getFunction(name) exists.
define void @f() {
  ret void
}
define void @f() {
  ret void
}
