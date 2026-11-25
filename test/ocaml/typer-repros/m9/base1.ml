(* queue 548: base.ml's include of Shadow_stdlib under a destructive type substitution: its aliases stay aliases *)
include (Shadow : module type of struct include Shadow end with module List := Shadow.List with type 'a ref := 'a ref)
