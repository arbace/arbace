module rec A : sig val f : int -> int val name : string Lazy.t end = struct
  let f n = if n = 0 then 0 else B.g (n - 1)
  let name = lazy "a"
end
and B : sig val g : int -> int end = struct let g n = A.f n + 1 end
let () = print_int (A.f 5); print_string (Lazy.force A.name); print_newline ()
