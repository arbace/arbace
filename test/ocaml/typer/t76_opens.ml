(* open of a module expression: open struct, open F(X), open (M : S), let open in expressions *)
module type S = sig val name : string end

open struct
  let hidden = "hidden"
end

let shown = hidden ^ "!"

module F (X : S) = struct let greet () = "hello " ^ X.name end
module N = struct let name = "n" end

let g =
  let open F (N) in
  greet ()

open (List : sig val length : 'a list -> int end)

let n = length [1; 2; 3]

let local =
  let open struct let k = 7 end in
  k * 6

let () = Printf.printf "%s %s %d %d\n" shown g n local
