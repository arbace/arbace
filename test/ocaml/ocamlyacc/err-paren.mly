/* Arbace's own sample (OC194 (a)): mismatched parentheses in an action */
%token A EOF
%start s
%type <unit> s
%%
s: A EOF { ignore (1 }; () } ;
