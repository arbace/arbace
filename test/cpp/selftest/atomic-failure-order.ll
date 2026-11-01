; report 043's F3: cmpxchg pairs with a failure ordering stronger than the success ordering (clang writes all 15 at -O0)
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
define i32 @f(ptr %p, i32 %v, i32 %c) {
  %a = cmpxchg ptr %p, i32 %c, i32 %v monotonic seq_cst, align 4
  %b = cmpxchg ptr %p, i32 %c, i32 %v monotonic acquire, align 4
  %d = cmpxchg ptr %p, i32 %c, i32 %v acquire seq_cst, align 4
  %e = cmpxchg ptr %p, i32 %c, i32 %v release acquire, align 4
  %g = cmpxchg weak ptr %p, i32 %c, i32 %v acq_rel seq_cst, align 4
  %h = cmpxchg ptr %p, i32 %c, i32 %v release monotonic, align 4
  ret i32 %c
}
