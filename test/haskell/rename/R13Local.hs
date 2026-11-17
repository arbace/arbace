module R13Local where

import R11Export (T(..), U(U2), (+++))

f :: Int -> T -> U
f n MkT = U2 (n +++ 1)
f n MkT2 = U2 n
