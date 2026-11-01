(* OCaml 5's effects (SPEC §4, effect patterns; §5, perform). *)
open Effect
open Effect.Deep

type _ Effect.t += Ask : int Effect.t | Tell : string -> unit Effect.t

let run f =
  match_with f ()
    { retc = (fun x -> x);
      exnc = raise;
      effc = (fun (type a) (e : a Effect.t) ->
        match e with
        | Ask -> Some (fun (k : (a, _) continuation) -> continue k 42)
        | Tell s -> print_endline s; Some (fun k -> continue k ())
        | _ -> None) }

let direct f =
  match f () with
  | v -> v
  | exception Not_found -> 0
  | effect Ask, k -> continue k 7
  | effect (Tell _), k -> continue k ()

let () = print_int (run (fun () -> perform Ask + 1))
