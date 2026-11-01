; report 043's F3 and CP174 (b): all 15 legal (success, failure) pairs, as clang writes them at -O0 (failure: monotonic, acquire, seq_cst)
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
define i32 @f(ptr %p, i32 %v, i32 %c) {
  %x0 = cmpxchg ptr %p, i32 %c, i32 %v monotonic monotonic, align 4
  %x1 = cmpxchg ptr %p, i32 %c, i32 %v monotonic acquire, align 4
  %x2 = cmpxchg ptr %p, i32 %c, i32 %v monotonic seq_cst, align 4
  %x3 = cmpxchg weak ptr %p, i32 %c, i32 %v acquire monotonic, align 4
  %x4 = cmpxchg ptr %p, i32 %c, i32 %v acquire acquire, align 4
  %x5 = cmpxchg ptr %p, i32 %c, i32 %v acquire seq_cst, align 4
  %x6 = cmpxchg ptr %p, i32 %c, i32 %v release monotonic, align 4
  %x7 = cmpxchg weak ptr %p, i32 %c, i32 %v release acquire, align 4
  %x8 = cmpxchg ptr %p, i32 %c, i32 %v release seq_cst, align 4
  %x9 = cmpxchg ptr %p, i32 %c, i32 %v acq_rel monotonic, align 4
  %x10 = cmpxchg ptr %p, i32 %c, i32 %v acq_rel acquire, align 4
  %x11 = cmpxchg weak ptr %p, i32 %c, i32 %v acq_rel seq_cst, align 4
  %x12 = cmpxchg ptr %p, i32 %c, i32 %v seq_cst monotonic, align 4
  %x13 = cmpxchg ptr %p, i32 %c, i32 %v seq_cst acquire, align 4
  %x14 = cmpxchg ptr %p, i32 %c, i32 %v seq_cst seq_cst, align 4
  ret i32 %c
}
