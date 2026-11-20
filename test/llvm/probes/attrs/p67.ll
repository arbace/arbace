; verdict: invalid invalid value for 'sign-return-address' attribute: bogus
; rule: llvm/lib/IR/Verifier.cpp:2535 Must be none, all or non-leaf.
define void @f() "sign-return-address"="bogus" { ret void }
