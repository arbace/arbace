(* effect handlers in the effect syntax: match ... with effect, try ... with effect, continue and discontinue *)
open Effect
open Effect.Deep

type _ Effect.t += Ask : int Effect.t | Say : string -> unit Effect.t

exception Stop

let run f =
  match f () with
  | v -> Printf.printf "value %d\n" v
  | exception Stop -> print_endline "stopped"
  | effect Ask, k -> continue k 42
  | effect (Say s), k -> print_endline s; continue k ()

let () = run (fun () -> perform (Say "hello"); perform Ask + 1)
let () = run (fun () -> perform (Say "bye"); raise Stop)

let count () =
  let n = ref 0 in
  (try
     perform (Say "a"); perform (Say "b"); !n
   with effect (Say _), k -> incr n; continue k ())
  |> Printf.printf "said %d\n"

let () = count ()

let () =
  match (fun () -> perform Ask) () with
  | x -> Printf.printf "x = %d\n" x
  | effect Ask, k -> (try discontinue k Stop with Stop -> print_endline "discontinued")
