let sub ~from ~amount = from - amount
let a = sub ~amount:3 ~from:10
let b = sub ~from:10 ~amount:4
let f ~x y = x * y
let g = f ~x:7
let () = print_int a; print_int b; print_int (g 2); print_newline ()
