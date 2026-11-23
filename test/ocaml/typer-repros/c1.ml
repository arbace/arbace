type 'a _types = 'a constraint 'a = < .. >
type 'a get_case = 'x constraint 'a _types = < case : 'x; .. >
type ('f, 't) mf = { c : 'f get_case -> 't get_case }
type ta = < case : int >
type tb = < case : string >
let m : (ta, tb) mf = { c = string_of_int }
let () = print_endline (m.c 42)
