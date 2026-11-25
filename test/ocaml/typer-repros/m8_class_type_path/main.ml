(* queue 547: a value whose type names another unit's class (A.iter), re-exported by a unit without an interface (B's `include C`),
   its signature kept as data: the class's object type is the path A.iter (Env's components), found again from B's .cmi;
   ppxlib's `assert_no_attributes_in : Ast_traverse0.iter` through Ppxlib's `include Common` *)
let () = B.x#core_type 1
