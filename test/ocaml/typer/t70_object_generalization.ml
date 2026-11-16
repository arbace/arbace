(* an object's methods generalized (Typecore.is_nonexpansive's objects), #-patterns of a parameterized variant in methods, a class type's name on a closed object printed as the abbreviation *)
type 'a expr = [ `Num of int | `Neg of 'a ]
let o = object (self)
    method map ~f = function
      | `Num _ as x -> x
      | `Neg x as e -> let x' = f x in if x == x' then e else `Neg x'
    method subst = function
      | #expr as e -> self#map ~f:(fun x -> x) e
  end
class type ['a, 'b] ops =
  object
    method free : 'b -> int
    method eval : 'b -> 'a
  end
type var = [`Var of string]
let var = object (self : ([>var], var) #ops)
  method free (`Var s) = String.length s
  method eval (#var as v) = v
end
let () = print_int (var#free (`Var "abc")); print_newline ()
