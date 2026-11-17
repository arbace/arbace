type 'a tree = Leaf | Node of 'a tree * 'a * 'a tree
let rec insert x = function
  | Leaf -> Node (Leaf, x, Leaf)
  | Node (l, v, r) as t -> if x < v then Node (insert x l, v, r) else if x > v then Node (l, v, insert x r) else t
let rec to_list = function Leaf -> [] | Node (l, v, r) -> to_list l @ (v :: to_list r)
let t = List.fold_left (fun t x -> insert x t) Leaf [5; 3; 8; 1; 4; 9]
let () = List.iter (fun x -> print_int x; print_char ' ') (to_list t); print_newline ()
