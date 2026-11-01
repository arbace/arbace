(* Arbace's own sample: a signature (SPEC §7). *)
val x : int
val f : ?opt:int -> lbl:string -> 'a list -> 'a option
external id : 'a -> 'a = "%identity"
type t = A | B of int
type 'a r = { f : 'a }
module M : sig type u end
module type S = sig val y : int end
module N : S with type t = int
module F : functor (X : S) -> S
module G (X : S) : S
exception E of string
include S
open List
class c : object method m : int end
class type ct = object end
type e = ..
type e += X
[@@@warning "-a"]
module rec A : sig end and B : sig end
module X := M
type u := t
module type T = module type of M
