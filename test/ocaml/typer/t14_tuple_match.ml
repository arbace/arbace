let cmp a b = match (a, b) with
  | (0, 0) -> "both"
  | (0, _) -> "first"
  | (_, 0) -> "second"
  | _ -> "neither"
let () = List.iter (fun (a, b) -> print_string (cmp a b); print_newline ()) [(0, 0); (0, 1); (1, 0); (1, 1)]
