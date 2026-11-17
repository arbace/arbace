exception Found of int
let find p l = try List.iter (fun x -> if p x then raise (Found x)) l; None with Found x -> Some x
let rec len acc = function [] -> acc | _ :: t -> len (acc + 1) t
let () = match find (fun x -> x mod 3 = 0) [1; 4; 9; 2] with Some v -> print_int (v + len 0 [1;2]) | None -> ()
let () = print_newline ()
