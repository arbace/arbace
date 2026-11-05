(* optional class parameters with defaults (Typeclass's *opt* rewriting) *)
class c ?(x = 1) ?y () = object
  method x = x
  method y = match y with Some v -> v | None -> 0
end
let () = print_int ((new c ())#x + (new c ~x:5 ~y:2 ())#y); print_newline ()
