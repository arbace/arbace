; verdict: invalid invalid value for 'branch-target-enforcement' attribute: x
; rule: llvm/lib/IR/Verifier.cpp:2553 Value must be empty, true or false.
define void @f() "branch-target-enforcement"="x" { ret void }
