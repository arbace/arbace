module ParseRecDo where
f = mdo
  rec x <- y
  return x
g = rec x <- y
