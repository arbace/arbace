type 'a _types = 'a constraint 'a = < case : _ >
type 'a get_case = 'x constraint 'a _types = < case : 'x; .. >
type _ w = ..
module type V = sig type case type types = < case : case > _types type _ w += Version : types w end
type 'types ov = (module V with type case = 'types get_case)
type ('f, 't) mf = { c : 'f get_case -> 't get_case }
type 'f im = Im : ('f, 't) mf * 't ov -> 'f im
module B = struct type case = string type types = < case : case > _types type _ w += Version : types w end
let g (im : < case : int > im) : (< case : int >, B.types) mf option =
  match im with
  | Im (f, (module To)) -> (match To.Version with B.Version -> Some f | _ -> None)
let () = match g (Im ({ c = string_of_int }, (module B))) with Some m -> print_endline (m.c 7) | None -> print_endline "none"
