; verdict: invalid PHINode should have one entry for each predecessor of its parent basic block!
; rule: llvm/lib/IR/Verifier.cpp:3383 Block b has predecessors e and a but the phi has one incoming value; checked in visitBasicBlock.
define i32 @f(i1 %c) {
e:
  br i1 %c, label %a, label %b
a:
  br label %b
b:
  %p = phi i32 [ 1, %a ]
  ret i32 %p
}
