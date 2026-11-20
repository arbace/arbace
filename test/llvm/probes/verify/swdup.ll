; verdict: invalid duplicate case value in switch
; rule: llvm/lib/AsmParser/LLParser.cpp:8005 Two identical i32 1 cases (uniqued ConstantInt).
define void @f(i32 %x) {
  switch i32 %x, label %a [ i32 1, label %a
    i32 1, label %a ]
a:
  ret void
}
