; verdict: invalid expected scope value for catchpad
; rule: llvm/lib/AsmParser/LLParser.cpp:8318 The parser requires a local value after catchpad within, not none.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 invoke void @g() to label %ok unwind label %cs
ok:
 ret void
cs:
 %c = catchswitch within none [label %cp] unwind to caller
cp:
 %p = catchpad within none [ptr null, i32 0, ptr null]
 catchret from %p to label %ok
}
