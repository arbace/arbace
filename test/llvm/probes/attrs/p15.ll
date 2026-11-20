; verdict: invalid Attribute 'byval' does not support unsized types!
; rule: llvm/lib/IR/Verifier.cpp:2171 byval of an opaque struct: the type is not sized.
%opq = type opaque
define void @f(ptr byval(%opq) %p) { ret void }
