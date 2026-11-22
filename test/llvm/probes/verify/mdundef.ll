; verdict: invalid use of undefined metadata '!7'
; rule: llvm/lib/AsmParser/LLParser.cpp:338 Unknown kind !foo is allowed but !7 is never defined; ForwardRefMDNodes check in validateEndOfModule.
define void @f() {
  ret void, !foo !7
}
