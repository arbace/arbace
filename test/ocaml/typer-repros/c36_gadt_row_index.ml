(* queue 547: a GADT whose index is an existential's row, refined by a pattern (Ctype.unify3's Tvariant pair in pattern mode:
   the rows reified, an equation recorded); ppx_expect's Test_node, `match behavior with Expect ... | Unreachable ...` *)
module Behavior = struct
  type _ t = E : { p : int } -> [ `E ] t | U : { q : int } -> [ `U ] t
end
type 'b r = { behavior : 'b Behavior.t }
type c = N : [< `E | `U ] r -> c
let tag (N { behavior }) = match behavior with E { p } -> p | U { q } -> q
let () = print_int (tag (N { behavior = Behavior.E { p = 3 } }))
