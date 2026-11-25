(* queue 547: a record copied from (`with`) whose type is unknown, its label out of scope, disambiguated by the expected type
   (Typecore's Pexp_record: the expected record type first); ppxlib_jane's `Ast_helper.with_default_loc { loc with loc_ghost = true }` *)
module A = struct
  module M = struct type loc = { s : int; ghost : bool } end
  type location = M.loc
  let with_loc (l : location) f = if l.M.ghost then f l.M.s else f 0
end
let k loc : A.location = { loc with ghost = true }
let g ~loc = A.with_loc { loc with ghost = true } (fun s -> s + 1)
let () = print_int (g ~loc:(k { A.M.s = 1; ghost = false }))
