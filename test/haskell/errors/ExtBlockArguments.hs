module ExtBlockArguments where
f = when True do
  pure ()
g = foo \x -> x
h = bar case 1 of _ -> 2
