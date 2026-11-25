type 'a _types = 'a constraint 'a = < case : _; pat : _ >
type 'a get_case = 'x constraint 'a _types = < case : 'x; .. >
type 'a get_pat = 'x constraint 'a _types = < pat : 'x; .. >
type ('f, 't) mf = { c : 'f get_case -> 't get_case; p : 'f get_pat -> 't get_pat }
module type AST = sig type case type pat end
module type V = sig module Ast : AST type types = < case : Ast.case; pat : Ast.pat > _types end
module M (A : V) (B : V) (F : sig val c : A.Ast.case -> B.Ast.case val p : A.Ast.pat -> B.Ast.pat end) = struct
  let m : (A.types, B.types) mf = { c = F.c; p = F.p }
end
module VA = struct module Ast = struct type case = int type pat = bool end type types = < case : int; pat : bool > end
module VB = struct module Ast = struct type case = string type pat = char end type types = < case : string; pat : char > end
module X = M (VA) (VB) (struct let c = string_of_int let p b = if b then 'y' else 'n' end)
let () = print_endline (X.m.c 42); print_char (X.m.p true); print_newline ()
