let f ~a ~b c = a * 100 + b * 10 + c
let g = f ~b:2
let h = g 3 ~a:1
let k = List.map (f ~a:1 ~b:1) [1; 2]
let () = print_int h; List.iter print_int k; print_newline ()
