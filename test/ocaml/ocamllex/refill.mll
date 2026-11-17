(* Arbace's own sample: a refill handler, arguments, a trailer and line directives *)
{ open Printf }
refill {fun k lexbuf -> k lexbuf}
rule main n m = parse
  | ['a'-'z']+ as w { printf "%d %d %s\n" n m w; main (n+1) m lexbuf }
# 40 "virtual.mll"
  | ' ' { main n (m+1) lexbuf }
  | '\'' { main n m lexbuf } (* ' *)
  | "\u{e9}" { main n m lexbuf }
  | eof { n }
  | _ { main n m lexbuf }
{ let () = ignore (main 0 0 (Lexing.from_string "ab cd")) }
