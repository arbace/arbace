# 10 "generated.mly"
/* Arbace's own sample (OC194 (a)): line directives, // comments, %term and %binary, %< %0 %2, \\ as %, = actions */
%{ let header = 1 %}
%term <int> X
%term Y Z EOF
%binary Y
%< Z
%0 W
%2 V
%start s
%type <int> s
%%
# 40 "elsewhere.mly"
s:
    t EOF       { $1 } // a line comment
;
t:
    X           { $1 + header }
  | t Y t       { $1 * $3 }
  | t Z t       = $1 - $3 }
  | W t V       { $2 }
;
\\
let trailer = ()
