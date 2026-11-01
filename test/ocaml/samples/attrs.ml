(* Attributes, extension points and docstrings (SPEC §8). *)
[@@@warning "-a"]
[@@@ocaml.text "floating"]
let x = 1 [@attr] [@@decl_attr 1]
let y = (f [@inline]) 1
let z = [%ext 1 + 2]
let%ext w = 3
[%%item let a = 1]
[%%item: int]
[%%item? Some x when x > 0]
type t = int [@@deriving show, eq]
type u = A [@a] | B of int [@b]
type 'a v = { f : int [@f] }
(** doc comment *)
let documented = 1
let e = [%ext: int -> int]
module M = struct end [@@mattr]
module type S = sig end [@@mt]
exception E [@@ex]
external ext : int -> int = "prim" [@@noalloc]
let p = function [%pat? Some _] -> () | _ -> ()
let ty : [%ty] = 1
class c = object [@@@cattr] method m = 1 [@@mattr] end
let () = ignore (fun [@attr] x -> x)
let q = (1 [@a] [@b])
