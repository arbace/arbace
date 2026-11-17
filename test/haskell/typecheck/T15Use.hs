module T15Use where

import T14Lib

shapes = [Square 2, Tri 3 4]

report = map (\s -> label s ++ ": " ++ show (areaOf s)) shapes

doubleArea s = scaled 2 s

queue = push 3 (push 2 (push 1 emptyQ))

firstOut = fmap fst (pop queue)

describe :: Named a => a -> Label
describe x = label x ++ " of " ++ show (areaOf x)

sumAll = totalArea shapes
