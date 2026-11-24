type 'a _types = 'a constraint 'a = < case : _; pat : _ >
type 'a get_case = 'x constraint 'a _types = < case : 'x; .. >
type 'a get_pat = 'x constraint 'a _types = < pat : 'x; .. >
module type AST = sig module Parsetree : sig type case type pat end end
module type V = sig
  module Ast : AST
  type types = < case : Ast.Parsetree.case; pat : Ast.Parsetree.pat > _types
  val show : Ast.Parsetree.case -> string
end
type 'types ov = (module V with type Ast.Parsetree.case = 'types get_case and type Ast.Parsetree.pat = 'types get_pat)
let f (type c) (type p) ((module A) : < case : c; pat : p > ov) (x : c) : string = A.show x
module BB = struct
  module Ast = struct module Parsetree = struct type case = string type pat = char end end
  type types = < case : Ast.Parsetree.case; pat : Ast.Parsetree.pat > _types
  let show s = "BB " ^ s
end
module CC = struct
  module Ast = struct module Parsetree = struct type case = int type pat = bool end end
  type types = < case : Ast.Parsetree.case; pat : Ast.Parsetree.pat > _types
  let show = string_of_int
end
let () = print_endline (f (module BB) "x"); print_endline (f (module CC) 5)
