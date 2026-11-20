; verdict: invalid '%a' is not a basic block
; rule: llvm/lib/AsmParser/LLParser.cpp:1859 br label %a where %a is an i32 argument: checkValidVariableType with label type.
define void @f(i32 %a) {
  br label %a
}
