(* queue 547: a module bound to a submodule of a functor's parameter is no alias (Env.is_functor_arg), included from the
   functor's application; ppx_compare_expander's `module Attrs = Attrs` under `open Params`, Compare's `include Make (Compare_params)` *)
module type A = sig val name : string end
module type P = sig module Attrs : A end
module P1 : P = struct module Attrs = struct let name = "one" end end
module Make (Params : P) = struct
  open Params
  module Attrs = Attrs
  let n = Attrs.name
end
module C = struct
  include Make (P1)
  let m = n ^ "!"
end
let () = print_string (C.Attrs.name ^ C.m)
