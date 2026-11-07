(* a local module's types leaving its scope, constructors chosen by the expected type, %apply without an inferred
   function (__FUNCTION__'s scopes are the corpus's translprim/locs: the toplevel names them otherwise; a polymorphic
   field bound by a pattern is lib-format/domains: its declaration's polytype prints as Printtyp does only with TY22) *)
external ( @@ ) : ('a -> 'b) -> 'a -> 'b = "%apply"
let twice f x = f (f x)


module type T = sig type a val v : a end
let unpack (type s) (m : (module T with type a = s)) = let module X = (val m) in X.v
let mk (type s) (x : s) : (module T with type a = s) = (module struct type a = s let v = x end)

module Shape = struct type t = Circle | Square end
let name (s : Shape.t) = match s with Circle -> "circle" | Square -> "square"

let () =
  print_endline (string_of_int (twice succ @@ 40));
  print_endline (unpack (mk "unpacked"));
  print_endline (name Shape.Square)
