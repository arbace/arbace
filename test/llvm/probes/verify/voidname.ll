; verdict: invalid instructions returning void cannot have a name
; rule: llvm/lib/AsmParser/LLParser.cpp:4063 %x = call void @g().
declare void @g()
define void @f() {
  %x = call void @g()
  ret void
}
