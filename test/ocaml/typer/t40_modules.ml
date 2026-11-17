module Counter = struct
  type t = { mutable n : int }
  let make () = { n = 0 }
  let incr c = c.n <- c.n + 1
  let get c = c.n
  exception Empty
end

module Pair = struct
  let swap (a, b) = (b, a)
  module Inner = struct
    let twice f x = f (f x)
  end
end

let c = Counter.make ()
let () = Counter.incr c; Counter.incr c
let () = print_int (Counter.get c); print_newline ()
let p = Pair.swap (1, "a")
let () = print_int (Pair.Inner.twice (fun x -> x * 3) 2); print_newline ()
