module R12UseExport (module R11Export, g1) where

import R11Export hiding (f1)
import qualified R11Export as E (f1)

g1 :: Int
g1 = E.f1 +++ 2 + fromMaybe 0 (Just 1)

x :: T
x = MkT
