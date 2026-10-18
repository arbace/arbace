-- | The boot file of Boot (R6): what User sees of Boot before Boot is compiled.
module Boot where

import Data.Kind (Type)

data Tree (a :: Type)

leaf :: a -> Tree a
size :: Tree a -> Int

instance Show a => Show (Tree a)
