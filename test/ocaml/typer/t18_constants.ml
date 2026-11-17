let classify = function
  | 0 -> "zero"
  | 1 | 2 -> "small"
  | 10 -> "ten"
  | _ -> "other"
let () = List.iter (fun n -> print_endline (classify n)) [0; 1; 2; 5; 10]
