; verdict: invalid expected '('
; rule: llvm/lib/AsmParser/LLParser.cpp:3450 initializes() has no range: the parser requires '(' for the first range.
define void @f(ptr initializes() %p) { ret void }
