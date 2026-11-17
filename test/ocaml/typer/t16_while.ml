let collatz n =
  let n = ref n and steps = ref 0 in
  while !n <> 1 do
    if !n mod 2 = 0 then n := !n / 2 else n := 3 * !n + 1;
    incr steps
  done;
  !steps
let () = print_int (collatz 27); print_newline ()
