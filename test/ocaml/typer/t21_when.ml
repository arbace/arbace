let sign n = match n with
  | 0 -> "zero"
  | n when n < 0 -> "neg"
  | _ -> "pos"
let () = List.iter (fun n -> print_endline (sign n)) [-3; 0; 7]
