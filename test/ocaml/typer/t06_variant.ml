type shape = Circle of int | Rect of int * int | Empty
let area = function
  | Circle r -> 3 * r * r
  | Rect (w, h) -> w * h
  | Empty -> 0
let () = List.iter (fun s -> print_int (area s); print_newline ()) [Circle 2; Rect (3, 4); Empty]
