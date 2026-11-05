let rec sum = function [] -> 0 | x :: rest -> x + sum rest
let l = [1; 2; 3; 4]
let () = print_int (sum l); print_newline ()
let () = List.iter (fun x -> print_int x; print_string " ") (List.rev l); print_newline ()
let doubled = List.map (fun x -> x * 2) l
let () = print_int (List.fold_left ( + ) 0 doubled); print_newline ()
let () = print_int (List.length (l @ doubled)); print_newline ()
