type ('a, 'b) t = T : ('a, 'a) t
type ('a, 'b) equal = ('a, 'b) t
include Defs.Defns (struct type ('a, 'b) t = ('a, 'b) equal end)
module Lift (X : sig type 'a t end) = struct
  let lift (type a b) (T : (a, b) t) : (a X.t, b X.t) t = T
end
