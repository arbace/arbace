(* The module language (SPEC §7). *)
module type S = sig
  type t
  val x : t
  val f : t -> t
  external g : int -> int = "caml_g" "caml_g_unboxed" [@@noalloc]
  module M : sig type u end
  module type T = sig end
  module N := M
  type u := t
  include Set.S with type elt = t
  open List
end
module A = struct
  type t = int
  let x = 1
  let f y = y
end
module B : S with type t = int = A
module C = (A : S)
module F (X : S) (Y : sig end) = struct include X end
module G (_ : S) = struct end
module H () = struct end
module FA = F (A) (struct end)
module HA = H ()
module type ST = module type of A
module type W = S with type t := int and module M = A and module type T = S
module type Func = functor (X : S) -> S
module type Fu = functor () -> S
module rec R1 : sig val x : int end = struct let x = R2.y end
and R2 : sig val y : int end = struct let y = 1 end
module P = (val (module A : S) : S)
let f (module M : S) = M.x
let g () = let module L = A in L.x
let m = (module A : S)
open struct let hidden = 1 end
open! List
include A
include (A : sig type t end)
module Alias = List
module type Abs
module Unpacked = (val m)
