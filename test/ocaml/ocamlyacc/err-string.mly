/* Arbace's own sample (OC194 (a)): a string not closed on its line (ocamlyacc's strings take a line break only after a backslash) */
%token A EOF
%start s
%type <string> s
%%
s: A EOF { "abc
  def" } ;
