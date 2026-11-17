rule main = parse
  | ("a" | "bc") # "c" { 1 }
