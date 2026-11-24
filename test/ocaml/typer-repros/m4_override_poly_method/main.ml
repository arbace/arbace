let o = object (self) inherit [string list] C.fold as super
  method int i acc = string_of_int i :: acc
  method! infos f x acc = super#infos f x ("over" :: acc) end
let () = print_endline (String.concat "," (o#infos (fun s acc -> s :: acc) { B.v = "v"; n = 2 } []))
