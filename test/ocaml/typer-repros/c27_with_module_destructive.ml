module type L = sig
  type 'a t
  module I : sig module type S end
  module Let_syntax : sig
    module Let_syntax : sig module O : I.S end
  end
end
module type M = sig
  module Make (Intf : sig module type S end) (Impl : Intf.S) : L with type 'a t := 'a list with module I := Intf
end
module M : M = struct
  module Make (Intf : sig module type S end) (Impl : Intf.S) = struct
    module Let_syntax = struct module Let_syntax = struct module O = Impl end end
  end
end
module X = M.Make (struct module type S = sig val x : int end end) (struct let x = 3 end)
let () = print_int X.Let_syntax.Let_syntax.O.x; print_newline ()
