/* Arbace sample (OC194 (a)): CRLF line ends */
%token <int> N
%token EOF
%start s
%type <int> s
%%
s:
  N EOF { $1 }
;
%%
let x = 1
