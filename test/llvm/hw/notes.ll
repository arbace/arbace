; A hand-written module: comments, blank lines and the spellings LLVM takes in several ways.
source_filename = "notes.ll"
; RUN: llvm-as < %s | llvm-dis | FileCheck %s
;   CHECK: define i32 @sum


target triple = "x86_64-alpine-linux-musl"   ; the host's

%pair = type { i32,i32 }        ; no space after the comma
%"quoted.type" = type opaque

@counter = external global i32          ; external: written, as the default is
@limit   =   internal   constant i32 10, align 4
@"plain" = global double 0x400921FB54442D18   ; pi, by its bits
@half    = global float 1.5
@neg     = global float -0.000000e+00
@big     = global i64 u0x7fffffffffffffff
@arr = private unnamed_addr constant [3 x i8] c"ab\00"
@vis = default global i8 0

; a function with numbered values, the entry block unlabelled
define i32 @sum(i32, i32) #0 {
  %3 = add nsw i32 %0, %1      ; %3: the next number
  %4 = icmp sgt i32 %3, 0
  br i1 %4, label %5, label %7

5:                                                ; preds = %2
  %6 = mul i32 %3, 2
  ret i32 %6

7:
  ret i32   0
}

define void @inline_attrs(ptr %p) nounwind uwtable {
entry:
  ; a comment inside a body
  store i32 1, ptr %p, align 4   ; align written
  store volatile i32 2, ptr %p
  %v = load i32, ptr %p, !tbaa !2
  call void @ext(i32 %v) #1
  ret void
}

declare void @ext(i32)

define float @floats(float %x) {
  %a = fadd fast float %x, 1.000000e+00
  %b = fmul float %a, 0x3FF8000000000000
  %c = fsub float %b, 2.5
  ret float %c
}

attributes #0 = { noinline "frame-pointer"="all" }
attributes #1 = { nounwind }

!llvm.module.flags = !{ !0 }
!0 = !{i32 7, !"uwtable", i32 2}   ; spaces around the braces above
!1 = !{!"omnipotent char", !3,i64 0}
!2 = !{ !1, !1, i64 0 }
!3 = !{!"Simple C/C++ TBAA"}
