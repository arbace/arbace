; A bit-packed vector in memory (the Mac's review 190, finding 1): <8 x i1> stored then read as an i64 and as <8 x i1>,
; <16 x i1>'s alloc size, a <4 x i1> loaded from a byte. Judged against lli (bin/rsi lower-unit).
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-musl"

@fmt = private constant [6 x i8] c"%llx\0A\00"

declare i32 @printf(ptr, ...)

define i32 @main() {
  %p = alloca i64, align 8
  store i64 0, ptr %p, align 8
  %v = bitcast i8 165 to <8 x i1>
  store <8 x i1> %v, ptr %p, align 1
  %w = load i64, ptr %p, align 8
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %w)
  %b = load <8 x i1>, ptr %p, align 1
  %n = xor <8 x i1> %b, <i1 true, i1 false, i1 false, i1 false, i1 false, i1 false, i1 false, i1 true>
  %ni = bitcast <8 x i1> %n to i8
  %nz = zext i8 %ni to i64
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %nz)
  %sz = ptrtoint ptr getelementptr (<16 x i1>, ptr null, i64 1) to i64
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %sz)
  %q = alloca <16 x i1>, align 2
  %c = icmp ult <16 x i8> <i8 0, i8 9, i8 2, i8 9, i8 4, i8 9, i8 6, i8 9, i8 8, i8 9, i8 10, i8 9, i8 12, i8 9, i8 14, i8 9>, splat (i8 5)
  store <16 x i1> %c, ptr %q, align 2
  %cw = load i16, ptr %q, align 2
  %cz = zext i16 %cw to i64
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %cz)
  store i8 10, ptr %p, align 1
  %f = load <4 x i1>, ptr %p, align 1
  %e1 = extractelement <4 x i1> %f, i64 1
  %e2 = extractelement <4 x i1> %f, i64 2
  %e3 = extractelement <4 x i1> %f, i64 3
  %s1 = zext i1 %e1 to i64
  %s2 = zext i1 %e2 to i64
  %s3 = zext i1 %e3 to i64
  %t = shl i64 %s3, 8
  %t2 = shl i64 %s2, 4
  %u = or i64 %t, %t2
  %u2 = or i64 %u, %s1
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %u2)
  ret i32 0
}
