/* Arbace sample (OC194 (a)): a trailer without a final newline */
%token A
%start s
%type <unit> s
%%
s: A { () } ;
%%
let no_newline = ()