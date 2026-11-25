(* queue 548: base's Shadow_stdlib, two of its aliases and a type *)
module Bigarray = Stdlib.Bigarray
module List = Stdlib.List
type 'a ref = 'a Stdlib.ref = { mutable contents : 'a }
