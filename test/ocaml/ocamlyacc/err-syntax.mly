/* Arbace's own sample (OC194 (a)): a syntax error in the rules */
%token A EOF
%start s
%type <unit> s
%%
s:
	A EOF { () } ? A
;
