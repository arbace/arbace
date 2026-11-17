(* module types: include in signatures, with type and destructive substitution, with module, module type of, aliases *)
module type ELT = sig type t val compare : t -> t -> int end
module type COLL = sig
  type elt
  type t
  val empty : t
  val add : elt -> t -> t
  val elements : t -> elt list
end
module type COLL_EXT = sig include COLL val size : t -> int end

module MakeColl (E : ELT) : COLL_EXT with type elt := E.t = struct
  type t = E.t list
  let empty = []
  let rec add x = function
    | [] -> [x]
    | y :: r as l -> let c = E.compare x y in if c < 0 then x :: l else if c = 0 then l else y :: add x r
  let elements l = l
  let size = List.length
end

module IntElt = struct type t = int let compare = Int.compare end
module C = MakeColl (IntElt)
module type C_TYPE = module type of C
module D : C_TYPE = C
module Elt = IntElt
module type HAS_ELT = sig module E : ELT end
module H : HAS_ELT with module E = Elt = struct module E = Elt end

let c = C.add 3 (C.add 1 (C.add 2 C.empty))
let () =
  List.iter print_int (C.elements c); print_newline ();
  print_int (D.size (D.add 5 D.empty)); print_int (H.E.compare 1 2); print_newline ()
