/* Arbace's own sample (OC194 (a)): a polymorphic start symbol, and the warnings of a retyped and a reprecedenced token */
%token <int> A
%token <string> A
%left B
%right B
%token EOF
%start s
%type <'a list> s
%%
s: A EOF { [] } ;
