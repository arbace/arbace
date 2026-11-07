(* Simplif: static exits used once inlined, unused handlers dropped, aliases substituted *)
type shape = Circle of int | Rect of int * int | Square of int | Dot

let area = function
  | Circle r when r > 100 -> 0
  | Circle r -> 3 * r * r
  | Rect (w, 0) | Square w when w > 50 -> w
  | Rect (w, h) -> w * h
  | Square s -> s * s
  | Dot -> 0

let name s =
  match s with
  | "one" | "uno" -> 1
  | "two" -> 2
  | "three" -> 3
  | _ -> 0

let rec find p = function
  | [] -> None
  | x :: _ when p x -> Some x
  | _ :: rest -> find p rest

let () =
  List.iter (fun s -> print_int (area s); print_char ' ') [Circle 2; Circle 200; Rect (2, 3); Square 4; Dot];
  print_newline ();
  print_int (name "uno" + name "two" + name "three" + name "x"); print_newline ();
  (match find (fun x -> x > 2) [1; 2; 3; 4] with Some x -> print_int x | None -> print_string "none");
  print_newline ()
