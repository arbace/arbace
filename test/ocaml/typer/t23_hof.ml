let rec filter p = function [] -> [] | x :: r -> if p x then x :: filter p r else filter p r
let evens = filter (fun x -> x mod 2 = 0) [1; 2; 3; 4; 5; 6]
let () = List.iter (fun x -> print_int x) evens; print_newline ()
let apply_all fs x = List.map (fun f -> f x) fs
let () = List.iter print_int (apply_all [(fun x -> x + 1); (fun x -> x * x)] 7); print_newline ()
