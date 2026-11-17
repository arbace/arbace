rule main = parse
  | "a\q
" { 1 }
  | eof { 2 }
