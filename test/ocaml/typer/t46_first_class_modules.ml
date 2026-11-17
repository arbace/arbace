(* first-class modules: packing with a signature, unpacking, package types with constraints *)
module type SHOW = sig type t val show : t -> string val v : t end

module I = struct type t = int let show = string_of_int let v = 42 let extra = 0 end
module S = struct type t = string let show s = "\"" ^ s ^ "\"" let v = "hi" end

let items : (module SHOW) list = [(module I : SHOW); (module S)]

let show_all l = List.map (fun m -> let module M = (val m : SHOW) in M.show M.v) l

let int_show (module M : SHOW with type t = int) = M.show (M.v + 1)

let make n = (module struct type t = int let show = string_of_int let v = n end : SHOW with type t = int)

let () =
  print_string (String.concat " " (show_all items)); print_newline ();
  print_string (int_show (module I)); print_newline ();
  let module N = (val make 5) in
  print_string (N.show N.v); print_newline ()
