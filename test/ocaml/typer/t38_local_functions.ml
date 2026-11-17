(* Simplif: local functions fully applied in one tail scope become static handlers; applied lambdas are beta-reduced *)
let classify x =
  let small y = y * 10 in
  if x < 0 then small (-x) else if x < 10 then small x else x

let pick b x y =
  let k z = z + 1 in
  match b with
  | true -> k x
  | false -> k y

let twice x =
  let f z = z * 2 in
  f (f x)

let beta = (fun a b -> a + b) 3 4

let try_local x =
  let fail () = -1 in
  try if x > 5 then fail () else x / (x - 3) with Division_by_zero -> fail ()

let () =
  print_int (classify (-3)); print_newline ();
  print_int (classify 4); print_newline ();
  print_int (classify 42); print_newline ();
  print_int (pick true 1 2 + pick false 1 2); print_newline ();
  print_int (twice 5); print_newline ();
  print_int beta; print_newline ();
  print_int (try_local 3 + try_local 9 + try_local 4); print_newline ()
