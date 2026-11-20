; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:3597 Numeric label 3: checkValueID only rejects numbers below the next free ID (0), so it is accepted.
define void @f() {
3:
  ret void
}
