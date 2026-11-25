(* queue 547: a class of an opened module (B includes A's), inherited unqualified (Env.open_signature opens classes too);
   ppx_compare_expander's `inherit type_is_recursive` under `open Ppxlib` *)
open B
let o = object inherit ((c) 1) as super method w = super#v + 1 end
let () = print_int o#w
