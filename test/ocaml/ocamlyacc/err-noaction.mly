/* Arbace's own sample (OC194 (a)): a production without an action */
%token A EOF
%start s
%type <unit> s
%%
s: A EOF { () }
 | A
 ;
