(* queue 548: an explicitly polymorphic annotation with a wildcard (parsexp's Automaton_action: `let raise_error : type a b.
   (a, b) Automaton_state.t -> _ = ...`): the wildcard is the definition's type, so an application's arguments know their
   expected types (Foo disambiguated by M.t), and the let generalizes it (two uses at two result types) *)
module M = struct type t = Foo | Bar let raise (x : t) ~at_eof = ignore at_eof; ignore x; failwith "" end
let r : type a. a list -> _ = fun l ~at_eof reason -> ignore l; M.raise reason ~at_eof
let f () = r [] ~at_eof:false Foo
let g () = ((r [] ~at_eof:false Foo : int), (r [1] ~at_eof:true Bar : string))
let id : 'a. 'a -> 'a = fun x -> x
let h = (id 1, id "x")
let k : type a. int = 3
