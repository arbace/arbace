module M = struct type t = { a : int; b : int } end
let f (x : M.t) = { x with a = 1; M.b = 2 }
let g = List.fold_left (fun r v -> { r with a = v; M.b = v }) { M.a = 0; b = 0 } [1; 2]
let () = Printf.printf "%d %d %d\n" (f { M.a = 5; b = 6 }).M.a g.M.a g.M.b
