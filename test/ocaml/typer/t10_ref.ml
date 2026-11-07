let r = ref 10
let () = r := !r * 2; incr r; decr r; decr r
let total = ref 0
let () = for i = 1 to 10 do total := !total + i done
let () = print_int !r; print_newline (); print_int !total; print_newline ()
