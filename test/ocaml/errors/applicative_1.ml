module type S = sig
  module F (X : sig end) : sig type t end
  type u = F(Y).t
end
