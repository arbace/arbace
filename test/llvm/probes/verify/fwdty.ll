; verdict: invalid instruction forward referenced with type 'i32'
; rule: llvm/lib/AsmParser/LLParser.cpp:4100 ret i32 %x forward-references %x as i32; later %x = add i64 defines it with type i64 (setInstName).
define i32 @f() {
  br label %b
a:
  ret i32 %x
b:
  %x = add i64 1, 2
  br label %a
}
