; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3077 cc17 lexes as 'cc' 17 = CXX_FAST_TLS; visitFunction's switch has no restriction for it (default/break).
define cc17 void @f() { ret void }
