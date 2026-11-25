module C = struct type t = Continue | Stop end
let rec loop n ~f : C.t =
  if n = 0 then Continue
  else match loop (n - 1) ~f with
    | Stop -> Stop
    | Continue -> f n
let () = print_endline (match loop 3 ~f:(fun n -> if n = 2 then C.Stop else C.Continue) with C.Stop -> "stop" | C.Continue -> "continue")
