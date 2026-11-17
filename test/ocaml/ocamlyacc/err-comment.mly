/* Arbace's own sample (OC194 (a)): an unterminated comment in the header */
%{
  let x = 1 (* never closed
%}
%token A EOF
%start s
%type <unit> s
%%
s: A EOF { () } ;
