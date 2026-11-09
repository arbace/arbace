/* Arbace's own sample (OC194 (a)): an undefined symbol */
%token A EOF
%start s
%type <unit> s
%%
s: A missing EOF { () } ;
