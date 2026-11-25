(* queue 547: a with-constraint on a submodule's type (Typemod.merge_constraint through the submodule), ppx_expect's
   Expect_test_config: `include Expect_test_config_types.S with type 'a IO.t = 'a` *)
module type S = sig
  module IO : sig type 'a t val return : 'a -> 'a t end
  val run : (unit -> unit IO.t) -> unit
end
module M : S with type 'a IO.t = 'a = struct
  module IO = struct type 'a t = 'a let return x = x end
  let run f = f ()
end
let () = M.run (fun () -> print_string (M.IO.return "ok"))
