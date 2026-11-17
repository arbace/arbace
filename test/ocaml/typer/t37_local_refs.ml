(* Simplif: local references become mutable variables (mutlet), a captured one stays a block *)
let sum_to n =
  let total = ref 0 in
  for i = 1 to n do total := !total + i done;
  !total

let count_evens l =
  let c = ref 0 in
  List.iter (fun x -> if x mod 2 = 0 then incr c) l;
  !c

let countdown n =
  let k = ref n and steps = ref 0 in
  while !k > 0 do decr k; incr steps done;
  !steps

let floats () =
  let f = ref 1.5 in
  f := !f *. 2.0;
  !f

let () =
  print_int (sum_to 10); print_newline ();
  print_int (count_evens [1; 2; 3; 4; 6]); print_newline ();
  print_int (countdown 7); print_newline ();
  print_float (floats ()); print_newline ()
