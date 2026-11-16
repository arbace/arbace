let f x =
  let y = x * 2 in
  let z = y + 1 in
  let (a, b) = (z, y) in
  a - b
let () = print_int (f 5); print_newline ()
