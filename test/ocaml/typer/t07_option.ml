let find_first p l =
  let rec go = function [] -> None | x :: r -> if p x then Some x else go r in
  go l
let show = function None -> print_string "none" | Some n -> print_int n
let () = show (find_first (fun x -> x > 2) [1; 2; 3; 4]); print_newline ()
let () = show (find_first (fun x -> x > 9) [1; 2]); print_newline ()
