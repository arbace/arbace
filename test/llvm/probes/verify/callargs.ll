; verdict: invalid argument is not of expected type 'i32'
; rule: llvm/lib/AsmParser/LLParser.cpp:8851 Explicit function type void (i32) gives expected param i32, the argument is i64 1.
declare void @g(i32)
define void @f() {
  call void (i32) @g(i64 1)
  ret void
}
