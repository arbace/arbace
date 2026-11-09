/* Arbace's own sample (OC194 (a)): $3 unbound */
%token <int> A
%token EOF
%start s
%type <int> s
%%
s: A EOF { $1 + $3 } ;
