; CP121 (c): every atomic form clang writes (bin/cppi selftest atomics): atomicrmw (fetch ops, xchg of an integer and a pointer, a
; loop for umax and fadd), cmpxchg weak and strong, atomic loads and stores (volatile too), fences (a singlethread one)
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
define i32 @f(ptr %p, i32 %v, i32 %c) {
  %a = atomicrmw volatile add ptr %p, i32 %v acq_rel, align 4
  %b = atomicrmw xchg ptr %p, i32 %v syncscope("singlethread") monotonic, align 4
  %x = cmpxchg weak ptr %p, i32 %c, i32 %v seq_cst monotonic, align 4
  %y = cmpxchg volatile ptr %p, i32 %c, i32 %v acquire acquire, align 4
  %l = load atomic i32, ptr %p acquire, align 4
  %l2 = load atomic volatile i32, ptr %p seq_cst, align 4
  store atomic i32 %l, ptr %p release, align 4
  store atomic volatile i32 %l, ptr %p unordered, align 4
  fence seq_cst
  fence syncscope("singlethread") acquire
  %fa = atomicrmw fadd ptr %p, float 1.0 seq_cst, align 4
  %m = atomicrmw umax ptr %p, i32 %v seq_cst, align 4
  %pp = atomicrmw xchg ptr %p, ptr null seq_cst, align 8
  %bb = atomicrmw add ptr %p, i8 1 seq_cst, align 1
  ret i32 %a
}
