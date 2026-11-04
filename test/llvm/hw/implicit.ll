; implicit numbering: unnamed blocks and an unnamed argument
source_filename = "implicit.ll"
define i32 @f(i32 %x, i32) {
  %2 = icmp eq i32 %x, 0
  br i1 %2, label %3, label %4
3:
  br label %4
4:
  %5 = phi i32 [ 1, %3 ], [ %0, %1 ]
  ret i32 %5
}

define void @g() {
  br label %next
next:                                             ; a named block after the entry
  ret void
}

; switch laid out on one line
define i32 @sw(i32 %v) {
entry:
  switch i32 %v, label %d [ i32 1, label %a i32 2, label %b ]
a:
  ret i32 10
b:
  ret i32 20
d:
  ret i32 0
}
