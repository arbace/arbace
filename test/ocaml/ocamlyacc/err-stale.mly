/* Arbace's own sample (OC194 (a)): a symbol after an action, the error reported on the next line at the symbol's column */
%token A EOF
%start s
%type <unit> s
%%
s:	A EOF { () }			  	EOF
	t: A { () } ;
