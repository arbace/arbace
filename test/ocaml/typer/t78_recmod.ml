(* recursive modules whose signatures name themselves (Typemod.transl_recmodule_modtypes' approximation), a class
   among their components (Translmod.init_shape) *)
module rec Tree : sig
  type t = Leaf of int | Node of Forest.t
  val sum : t -> int
end = struct
  type t = Leaf of int | Node of Forest.t
  let sum = function Leaf n -> n | Node f -> Forest.sum f
end
and Forest : sig
  type t = Tree.t list
  val sum : t -> int
end = struct
  type t = Tree.t list
  let sum f = List.fold_left (fun a t -> a + Tree.sum t) 0 f
end

module rec Self : sig
  type 'a box = Box of 'a Self.box option | Full of 'a
  val depth : 'a box -> int
end = struct
  type 'a box = Box of 'a Self.box option | Full of 'a
  let depth = function Full _ -> 0 | Box None -> 1 | Box (Some b) -> 1 + Self.depth b
end

module rec C : sig
  class c : object method go : string end
  val make : unit -> c
end = struct
  class c = object method go = "go" end
  let make () = new c
end

module type REC = sig
  module rec A : sig val a : int -> int end
  and B : sig val b : int -> int end
end

let () =
  Printf.printf "%d %d %s\n" (Tree.sum (Node [Leaf 1; Node [Leaf 2; Leaf 3]]))
    (Self.depth (Box (Some (Box (Some (Full 'x')))))) ((C.make ())#go)
