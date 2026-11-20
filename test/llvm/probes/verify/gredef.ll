; verdict: invalid redefinition of global '@g'
; rule: llvm/lib/AsmParser/LLParser.cpp:1473 Second @g: M->getNamedValue(Name) already exists.
@g = global i32 0
@g = global i32 1
