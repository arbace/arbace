/* Arbace's own sample (OC194 (a)): no %start: the goal %entry% undefined */
%token A EOF
%%
s: A EOF { () } ;
