type point = { x : int; y : int }
let origin = { x = 0; y = 0 }
let move p dx = { p with x = p.x + dx }
let norm1 { x; y } = abs x + abs y
let () = print_int (norm1 (move origin (-5))); print_newline ()
