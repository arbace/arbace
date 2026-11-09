/* Arbace's own sample (OC194 (a)): conflicts resolved by precedence, %left, %right, %nonassoc, %prec */
%{
open Printf
let fact n = (* a comment with "a string" and 'c' *) if n < 2 then 1 else n
%}
%token <int> INT
%token <string> IDENT
%token PLUS MINUS TIMES DIV POW EQ LT LPAREN RPAREN COMMA EOF
%token IF THEN ELSE
%nonassoc below_ELSE
%nonassoc ELSE
%nonassoc EQ LT
%left PLUS MINUS
%left TIMES DIV
%right POW
%nonassoc UMINUS
%start main
%type <int> main
%%
main:
    expr EOF                   { $1 }
;
expr:
    INT                        { $1 }
  | IDENT                      { String.length $1 }
  | LPAREN expr RPAREN         { $2 }
  | expr PLUS expr             { $1 + $3 }
  | expr MINUS expr            { $1 - $3 }
  | expr TIMES expr            { $1 * $3 }
  | expr DIV expr              { if $3 = 0 then 0 else $1 / $3 }
  | expr POW expr              { let rec p a b = if b = 0 then 1 else a * p a (b - 1) in p $1 $3 }
  | expr EQ expr               { if $1 = $3 then 1 else 0 }
  | expr LT expr               { if $1 < $3 then 1 else 0 }
  | MINUS expr %prec UMINUS    { - $2 }
  | IF expr THEN expr %prec below_ELSE { if $2 <> 0 then $4 else 0 }
  | IF expr THEN expr ELSE expr        { if $2 <> 0 then $4 else $6 }
  | IDENT LPAREN args RPAREN   { fact (List.length $3) }
;
args:
    /* empty */                { [] }
  | expr                       { [$1] }
  | expr COMMA args            { $1 :: $3 }
;
%%
let () = ignore (sprintf "%d" 0)
