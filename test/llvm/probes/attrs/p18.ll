; verdict: invalid invalid mask value for 'nofpclass'
; rule: llvm/lib/AsmParser/LLParser.cpp:2929 nofpclass(0): numeric mask of zero is rejected.
define void @f(float nofpclass(0) %p) { ret void }
