module User (weight) where

import {-# SOURCE #-} Boot (Tree, size)

weight :: Tree a -> Int
weight t = 1 + size t
