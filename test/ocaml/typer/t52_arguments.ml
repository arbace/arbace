(* optional parameters eliminated when a function is passed as an argument (Typecore.type_argument), GADT lets *)
let apply f = f 10
let scale ?(by = 2) x = x * by
let label ?prefix x = match prefix with Some p -> p ^ string_of_int x | None -> string_of_int x

type (_, _) eq = Refl : ('a, 'a) eq
let coerce : type a b. (a, b) eq -> a list -> b list = fun e l -> let Refl = e in l

let () =
  print_int (apply scale); print_newline ();
  print_string (apply label); print_newline ();
  print_int (List.length (coerce Refl [1; 2; 3])); print_newline ();
  List.iter (fun s -> print_string s) (List.map label [1; 2]); print_newline ()
