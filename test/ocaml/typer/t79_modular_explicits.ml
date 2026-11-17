(* modular explicits (OCaml 5.5): dependent arrows (module M : S) -> ..., inferred and annotated, labelled, applied to
   module paths, to functor applications and to a structure (its types expanded: instance_funct_nondep) *)
module type Show = sig
  type t
  val show : t -> string
end

let show (module S : Show) (x : S.t) = S.show x
let show_twice : (module S : Show) -> S.t -> string = fun (module S) x -> S.show x ^ S.show x
let show_list ~m:(module S : Show) (l : S.t list) = String.concat "," (List.map S.show l)

module Int = struct type t = int let show = string_of_int end
module Pair (A : Show) (B : Show) = struct
  type t = A.t * B.t
  let show (a, b) = "(" ^ A.show a ^ " " ^ B.show b ^ ")"
end
module Str = struct type t = string let show s = s end

let () =
  print_endline (show (module Int) 42);
  print_endline (show_twice (module Str) "ab");
  print_endline (show_list ~m:(module Int) [1; 2; 3]);
  print_endline (show (module Pair (Int) (Str)) (1, "one"));
  print_endline (show (module struct type t = bool let show b = if b then "yes" else "no" end) true)
