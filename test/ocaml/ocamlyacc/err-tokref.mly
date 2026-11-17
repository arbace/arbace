/* Arbace's own sample (OC194 (a)): $2 refers to a token without an argument */
%token <int> A
%token EOF
%start s
%type <int> s
%%
s: A EOF { $1 + $2 } ;
