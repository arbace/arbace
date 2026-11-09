/* Arbace's own sample (OC194 (a)): a start symbol without a %type */
%token A EOF
%start s t
%type <unit> s
%%
s: A EOF { () } ;
t: A { () } ;
