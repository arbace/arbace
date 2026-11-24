let o = object inherit B.map method s : string B.T.map = fun s -> s ^ "!" end
let () = print_int (o#int 5); print_endline (o#s "x")
