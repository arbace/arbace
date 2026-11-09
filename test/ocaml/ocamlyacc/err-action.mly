/* Arbace's own sample (OC194 (a)): an unterminated action */
%token A EOF
%start s
%type <unit> s
%%
s:
    A EOF       { let x = { contents = 1 } in
                  ignore x; ()
;
