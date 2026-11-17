(* coercions (:>) to wider variant types, single and double, through lists and functions *)
type small = [ `A | `B ]
type big = [ `A | `B | `C ]

let widen (x : small) = (x :> big)
let widen2 x = (x : small :> big)
let widen_any x = (x :> big)
let widen_list (l : small list) = (l :> big list)
let narrow_fun (f : big -> int) = (f :> small -> int)

let to_int : big -> int = function `A -> 1 | `B -> 2 | `C -> 3

let () =
  print_int (to_int (widen `A)); print_newline ();
  print_int (to_int (widen2 `B)); print_newline ();
  print_int (to_int (widen_any `C)); print_newline ();
  print_int (List.fold_left (fun a x -> a + to_int x) 0 (widen_list [`A; `B; `B])); print_newline ();
  print_int (narrow_fun to_int `B); print_newline ()
