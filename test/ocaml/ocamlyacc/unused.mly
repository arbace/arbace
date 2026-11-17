/* Arbace's own sample (OC194 (a)): rules never reduced, a nonterminal only an unused rule reaches */
%token A B C EOF
%start s
%type <unit> s
%%
s:
    a EOF       { () }
;
a:
    A           { () }
  | A B         { () }
  | A           { () }
;
unreached:
    C           { () }
;
