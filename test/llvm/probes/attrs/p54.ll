; verdict: invalid 'vscale_range' minimum cannot be greater than maximum
; rule: llvm/lib/IR/Verifier.cpp:2511 vscale_range(4,2): minimum 4 is a power of two, but exceeds maximum 2.
define void @f() vscale_range(4,2) { ret void }
