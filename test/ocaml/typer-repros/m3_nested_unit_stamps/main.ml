let o = object (self) inherit [string list] C.fold as super method! attr x acc = string_of_int x :: super#attr x acc end
let () = print_endline (String.concat "," (o#attr 3 ["a"]))
