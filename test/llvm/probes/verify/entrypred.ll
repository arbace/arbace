; verdict: invalid Entry block to function must not have predecessors!
; rule: llvm/lib/IR/Verifier.cpp:3221 Entry block e branches to itself; visitFunction checks pred_empty(Entry) before visiting blocks.
define void @f() {
e:
  br label %e
}
