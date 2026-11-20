; verdict: invalid 'vscale_range' minimum must be power-of-two value
; rule: llvm/lib/IR/Verifier.cpp:2508 vscale_range(3,5): minimum 3 is not a power of two (checked first).
define void @f() vscale_range(3,5) { ret void }
