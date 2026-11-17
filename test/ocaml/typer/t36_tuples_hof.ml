let swap_all l = List.map (fun (a, b) -> (b, a)) l
let pairs = swap_all [(1, "a"); (2, "b")]
let () = List.iter (fun (s, n) -> print_string s; print_int n) pairs; print_newline ()
let count = ref 0
let () = List.iter (fun (_, n) -> count := !count + n) pairs; print_int !count; print_newline ()
