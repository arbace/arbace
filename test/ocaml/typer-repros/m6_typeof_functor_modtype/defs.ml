module Defns (Type_equal : sig type ('a, 'b) t end) = struct
  module type Lift = sig
    type 'a t
    val lift : ('a, 'b) Type_equal.t -> ('a t, 'b t) Type_equal.t
  end
end
