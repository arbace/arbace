(* Matching on variants: hashed tags in switches and test sequences, isint splits, or-patterns, guards, partial
   matches with Match_failure, nested tags *)
let many = function
  | `Alpha -> 1 | `Beta -> 2 | `Gamma -> 3 | `Delta -> 4 | `Epsilon -> 5 | `Zeta -> 6
  | `Eta n -> n | `Theta (a, b) -> a + b

let consts = function `One | `Two -> 12 | `Three -> 3 | `Four -> 4

let mixed = function
  | `K -> 0
  | `V x when x > 10 -> x
  | `V _ -> -1
  | `W s -> String.length s

let partial = function `P -> "p" | `Q -> "q"

let nested = function
  | `Outer (`Inner n) -> n
  | `Outer `Empty -> 0
  | `Other -> -1

let two x y = match x, y with
  | `L, `L -> "ll"
  | `L, `R -> "lr"
  | `R, _ -> "r_"

let () =
  List.iter (fun t -> print_int (many t); print_char ' ') [`Alpha; `Beta; `Gamma; `Delta; `Epsilon; `Zeta; `Eta 7; `Theta (4, 4)];
  print_newline ();
  List.iter (fun t -> print_int (consts t); print_char ' ') [`One; `Two; `Three; `Four]; print_newline ();
  List.iter (fun t -> print_int (mixed t); print_char ' ') [`K; `V 20; `V 1; `W "abc"]; print_newline ();
  print_endline (partial `P);
  print_int (nested (`Outer (`Inner 5)) + nested (`Outer `Empty) + nested `Other); print_newline ();
  print_endline (two `L `R ^ two `R `L ^ two `L `L);
  print_endline (try partial (Obj.magic (`Z : [`Z]) : [`P | `Q]) with Match_failure _ -> "match failure")
