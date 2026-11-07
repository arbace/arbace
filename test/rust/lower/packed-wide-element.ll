; a bit-packed vector whose elements are wider than a byte (<1 x i10>, <2 x i10>: rustc's simd-bitmask-notpow2): its value's
; C struct holds each element in its integer container, its memory the N*k bits (arbace.cpp.lower's packed-vector-c-size-align)
declare i32 @printf(ptr, ...)
@fmt = private constant [4 x i8] c"%d\0A\00"
@m = global [4 x i8] zeroinitializer, align 4

define i32 @main() {
  %v = bitcast i10 677 to <1 x i10>
  %e = extractelement <1 x i10> %v, i32 0
  %ew = zext i10 %e to i32
  call i32 (ptr, ...) @printf(ptr @fmt, i32 %ew)
  %w = insertelement <2 x i10> <i10 1, i10 2>, i10 1000, i32 1
  store <2 x i10> %w, ptr @m, align 4
  %l = load i32, ptr @m, align 4
  %lm = and i32 %l, 1048575
  call i32 (ptr, ...) @printf(ptr @fmt, i32 %lm)
  %back = load <2 x i10>, ptr @m, align 4
  %b1 = extractelement <2 x i10> %back, i32 1
  %b1w = zext i10 %b1 to i32
  call i32 (ptr, ...) @printf(ptr @fmt, i32 %b1w)
  %i = bitcast <2 x i10> %back to i20
  %iw = zext i20 %i to i32
  call i32 (ptr, ...) @printf(ptr @fmt, i32 %iw)
  ret i32 0
}
