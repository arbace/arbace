(* signatures: abstract types, coercions that reorder and drop fields, primitives, exceptions in signatures *)
module type STACK = sig
  type 'a t
  exception Empty
  val empty : 'a t
  val push : 'a -> 'a t -> 'a t
  val pop : 'a t -> 'a * 'a t
  val size : 'a t -> int
end

module Stack : STACK = struct
  exception Empty
  type 'a t = 'a list
  let helper = 42
  let size l = List.length l
  let empty = []
  let push x l = x :: l
  let pop = function [] -> raise Empty | x :: r -> (x, r)
end

module Ops : sig
  external add : int -> int -> int = "%addint"
  val twice : int -> int
  val neg : int -> int
end = struct
  let neg x = - x
  external add : int -> int -> int = "%addint"
  let twice x = add x x
end

module Plain : sig val add : int -> int -> int val name : string end = struct
  let name = "plain"
  external add : int -> int -> int = "%addint"
end

let s = Stack.push 1 (Stack.push 2 Stack.empty)
let (top, rest) = Stack.pop s
let () =
  print_int top; print_int (Stack.size rest); print_newline ();
  (try ignore (Stack.pop (snd (Stack.pop rest))) with Stack.Empty -> print_string "empty");
  print_newline ();
  print_int (Ops.add (Ops.twice 3) (Ops.neg 1)); print_int (Plain.add 1 2); print_string Plain.name;
  print_newline ()
