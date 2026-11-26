(* queue 548: a constructor of several arguments whose argument pattern is constrained by their tuple (ppx_log's
   `Pconst_integer (_ : string * char option)`): Typecore's Ppat_construct splits the inner pattern and unifies the
   constraint with the arguments' tuple *)
type t = A of int * char | B
let f = function A (_ : int * char) -> 1 | B -> 0
let g = function A ((x, y) : int * char) -> x + Char.code y | B -> 0
let h = function A ((x, _) : _ * char) -> x | B -> 0
let () = print_int (f (A (1, 'a')) + g (A (2, 'b')) + h B)
