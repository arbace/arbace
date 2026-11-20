; verdict: invalid invalid linkage for function definition
; rule: llvm/lib/AsmParser/LLParser.cpp:7089 extern_weak is not valid on a define.
define extern_weak void @f() {
  ret void
}
