type 'a _types = 'a constraint 'a = < case : _; pat : _ >
type 'a get_case = 'x constraint 'a _types = < case : 'x; .. >
type 'a get_pat = 'x constraint 'a _types = < pat : 'x; .. >
type _ w = ..
module type V = sig
  module Ast : sig type case type pat end
  type types = < case : Ast.case; pat : Ast.pat > _types
  type _ w += Version : types w
end
type 'types ov = (module V with type Ast.case = 'types get_case and type Ast.pat = 'types get_pat)
type ('f, 't) mf = { c : 'f get_case -> 't get_case; p : 'f get_pat -> 't get_pat }
type 'f im = Im : ('f, 't) mf * 't ov -> 'f im
module B = struct
  module Ast = struct type case = string type pat = char end
  type types = < case : Ast.case; pat : Ast.pat > _types
  type _ w += Version : types w
end
type ti = < case : int; pat : bool >
let g (im : ti im) : (ti, B.types) mf option =
  match im with
  | Im (f, (module To)) -> (match To.Version with B.Version -> Some f | _ -> None)
let () = match g (Im ({ c = string_of_int; p = (fun b -> if b then 'y' else 'n') }, (module B))) with
  | Some m -> print_endline (m.c 7); print_char (m.p true); print_newline () | None -> print_endline "none"
