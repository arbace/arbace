; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:7114 [2 x i32] is a first-class type and a valid return type; neither parser nor verifier rejects it.
define [2 x i32] @f() {
 ret [2 x i32] zeroinitializer
}
