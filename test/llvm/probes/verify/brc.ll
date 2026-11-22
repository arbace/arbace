; verdict: invalid branch condition must have 'i1' type
; rule: llvm/lib/AsmParser/LLParser.cpp:7962 br with an i32 condition fails in parseBr before the labels are parsed.
define void @f(i32 %c) {
  br i32 %c, label %a, label %a
a:
  ret void
}
