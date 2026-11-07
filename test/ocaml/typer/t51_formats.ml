(* Printf's formats: string literals typed as CamlinternalFormatBasics' constructors (Typecore.type_format) *)
let () = Printf.printf "%d %i %5d %-5d| %05d %x %X %#o %u\n" 42 (-7) 3 4 5 255 255 8 9
let () = Printf.printf "%s|%10s|%-6s|%S %c %C %b %B\n" "a" "right" "left" "q\"" 'z' 'y' true false
let () = Printf.printf ("%f %.2f %e %g " ^^ "%F %.3g\n") 3.14159 2.5 1234.5 0.0001 1.0 2.0
let pp_pair ppf (a, b) = Format.fprintf ppf "(%d,@ %d)" a b
let () = Format.printf "@[<hov 2>pair:@ %a@]@." pp_pair (1, 2)
let show = Printf.sprintf "%ld %Ld %nd %%" 1l 2L 3n
let k = Printf.ksprintf (fun s -> String.length s) "%s-%s" "ab" "cd"
let () = print_endline show; print_int k; print_newline ()
let () = Printf.printf "%t|%a\n" (fun oc -> output_string oc "t") (fun oc s -> output_string oc s) "a"
let () = Printf.printf "%*d|%.*f|%{%d%}\n" 4 7 1 2.25 "%d"
