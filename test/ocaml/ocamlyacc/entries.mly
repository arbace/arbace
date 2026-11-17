/* Arbace's own sample (OC194 (a)): several %start, the error token, types with arrows and brackets, token values,
   quoted strings {|...|} and {id|...|id}, character literals, nested comments (* (* *) *) and the $ in names */
%{
  let quoted = {|a } % } " |} and {foo| |bar} |} |foo}
  let chars = ['\'' ; '\\' ; '\065' ; '\x41' ; '\o101' ; 'a']
  (* a nested (* comment *) with a "string" *)
%}
%token <int * (int -> int)> PAIR
%token <[ `A | `B ] list> VARIANTS
%token <int> NUM 300
%token SEMI 301
%token LBRACE RBRACE EOF
%start list item
%type <int list> list
%type <(int -> int) option> item
%start other
%type <unit> other
%%
list:
    /* empty */          { [] }
  | NUM SEMI list        { $1 :: $3 }
  | error SEMI list      { $3 }
;
item:
    PAIR                 { let (_, f) = $1 in Some f }
  | VARIANTS             { ignore $1; None }
  | LBRACE item RBRACE   { (* { braces } in a comment *) let s = "}" in ignore s; $2 }
;
other:
    EOF                  { let x' = 'x' and y'' = {|}|} in ignore (x', y''); () }
  | SEMI other           { $2 }
;
%%
(* the trailer *)
let _ = list
