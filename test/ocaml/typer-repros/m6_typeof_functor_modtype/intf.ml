module type S = sig
  type ('a, 'b) t = T : ('a, 'a) t
  type ('a, 'b) equal := ('a, 'b) t
  include module type of Defs.Defns (struct type ('a, 'b) t = ('a, 'b) equal end)
  module Lift (X : sig type 'a t end) : Lift with type 'a t := 'a X.t
end
