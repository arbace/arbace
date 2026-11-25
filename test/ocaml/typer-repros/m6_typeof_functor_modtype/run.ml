module L = Main.Lift (struct type 'a t = 'a list end)
let () = match L.lift Main.T with Main.T -> print_endline "ok"
