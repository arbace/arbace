; verdict: invalid 'vscale_range' minimum must be greater than 0
; rule: llvm/lib/IR/Verifier.cpp:2506 vscale_range(0,4): minimum is 0.
define void @f() vscale_range(0,4) { ret void }
