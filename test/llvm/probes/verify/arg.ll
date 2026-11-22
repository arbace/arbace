; verdict: invalid argument expected to be numbered '%1' or greater
; rule: llvm/lib/AsmParser/LLParser.cpp:3598 Second unnamed %0 argument: checkValueID (called from parseArgumentList at 3651) sees ID 0 < next ID 1.
define void @f(i32 %0, i32 %0) { ret void }
