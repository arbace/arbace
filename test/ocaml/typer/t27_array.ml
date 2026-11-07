let a = Array.make 5 0
let () = for i = 0 to 4 do a.(i) <- i * i done
let () = print_int (a.(2) + a.(4)); print_newline ()
let () = print_int (Array.length a); print_newline ()
let b = [| 1; 2; 3 |]
let () = Array.iter print_int b; print_newline ()
