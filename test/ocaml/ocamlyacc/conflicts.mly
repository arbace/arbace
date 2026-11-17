/* Arbace's own sample (OC194 (a)): an unresolved shift/reduce conflict (the dangling else) and a reduce/reduce one */
%token <string> ID
%token IF THEN ELSE SEMI EOF
%start prog
%type <string list> prog
%%
prog:
    stmts EOF                       { List.rev $1 }
;
stmts:
    /* empty */                     { [] }
  | stmts stmt SEMI                 { $2 :: $1 }
;
stmt:
    IF ID THEN stmt                 { "if " ^ $2 ^ " then " ^ $4 }
  | IF ID THEN stmt ELSE stmt       { "if " ^ $2 ^ " then " ^ $4 ^ " else " ^ $6 }
  | lhs1                            { $1 }
  | lhs2                            { $1 }
;
lhs1:
    ID                              { "1:" ^ $1 }
;
lhs2:
    ID                              { "2:" ^ $1 }
;
