module R35Imports2 (module Data.Char, ord', Foo(..), bar) where

import Data.Char (ord, chr, isDigit)
import Data.List hiding (sort)
import qualified Data.List
import Data.Maybe as Mb
import Data.Either (either, lefts)
import Control.Monad (forM_, when, unless)
import Data.IORef ()

data Foo = Foo | Baz Int

ord' :: Char -> Int
ord' = ord

bar :: [Int] -> [Int]
bar xs = Data.List.sort (nub xs)

use :: Maybe Int -> Int
use = Mb.fromMaybe 0

io :: IO ()
io = forM_ [1 :: Int, 2] $ \i -> when (i > 1) (print i)
