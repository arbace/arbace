let kind c = match c with
  | 'a' .. 'z' -> "lower"
  | 'A' .. 'Z' -> "upper"
  | '0' .. '9' -> "digit"
  | _ -> "other"
let () = String.iter (fun c -> print_endline (kind c)) "aZ5!"
