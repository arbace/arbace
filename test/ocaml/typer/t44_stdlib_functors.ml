(* the stdlib's functors: Set.Make, Map.Make, Hashtbl.Make over the stdlib's modules and the program's own *)
module SS = Set.Make (String)
module IM = Map.Make (Int)

module Pt = struct
  type t = int * int
  let compare (a, b) (c, d) = if a = c then compare b d else compare a c
  let equal p q = compare p q = 0
  let hash (a, b) = a * 31 + b
end

module PS = Set.Make (Pt)
module PH = Hashtbl.Make (Pt)

let words = SS.of_list ["pear"; "apple"; "fig"; "apple"]
let m = IM.add 3 "three" (IM.add 1 "one" IM.empty)
let pts = PS.add (1, 2) (PS.singleton (0, 5))

let () =
  SS.iter (fun w -> print_string w; print_char ' ') words; print_newline ();
  IM.iter (fun k v -> print_int k; print_string v) m; print_newline ();
  print_int (PS.cardinal pts); print_newline ();
  let h = PH.create 8 in
  PH.replace h (1, 1) "a"; PH.replace h (2, 3) "b"; PH.replace h (1, 1) "c";
  print_int (PH.length h); print_string (PH.find h (1, 1)); print_newline ()
