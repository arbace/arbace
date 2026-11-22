; verdict: invalid Invalid (unordered or overlapping) range list
; rule: llvm/lib/AsmParser/LLParser.cpp:3471 initializes((4, 8), (0, 2)): second range is not above the first, so getConstantRangeList returns nullopt.
define void @f(ptr initializes((4, 8), (0, 2)) %p) { ret void }
