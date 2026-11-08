(* Arbace's own sample for the native ocamllex (doc/ocaml/OCAML-SURVEY.md, OC186): bindings
   the static optimisation cannot place, in alternatives and repetitions, so memory cells,
   their moves and a temporary cell appear; optional and character bindings *)
{
let pr = print_string
}

let digit = ['0'-'9']
let alpha = ['a'-'z' 'A'-'Z' '_']
let id = alpha (alpha | digit)*
let blank = [' ' '\t']

rule token = parse
  | (id as a) blank* '=' blank* ((digit+ as n) | (id as v)) ';'
      { pr a; (match n with Some n -> pr n | None -> ()); (match v with Some v -> pr v | None -> ()); token lexbuf }
  | ('a' as x) | ('b' as x) 'c' { ignore x; token lexbuf }
  | (('x' | "yy")* as s) 'z' (_ as c) { ignore (s, c); token lexbuf }
  | ((digit* as i) '.' (digit* as f))+ { ignore (i, f); token lexbuf }
  | "\"" ([^ '"' '\\']* ('\\' _ [^ '"' '\\']*)* as body) "\"" { ignore body; token lexbuf }
  | ['\x00'-'\x1f'] # ['\n'] { token lexbuf }
  | '\n' { Lexing.new_line lexbuf; token lexbuf }
  | eof { () }
  | _ { token lexbuf }

and comment depth = parse
  | "(*" { comment (depth + 1) lexbuf }
  | "*)" { if depth > 0 then comment (depth - 1) lexbuf }
  | ("{" (['a'-'z']* as d) "|") { ignore d; comment depth lexbuf }
  | eof { failwith "eof" }
  | _ { comment depth lexbuf }

and short = shortest
  | (_ as a) (_ as b)? { (a, b) }
  | "ab" { ('a', None) }

{
let () =
  let lb = Lexing.from_string "x = 12; y = zz; aaz \"a\\\"b\" 1.2.3" in
  token lb; print_newline ()
}
