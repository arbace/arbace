module R06Imports where

import Data.List (sortBy, nub, (\\))
import qualified Data.Maybe as M
import qualified Data.Map as Map
import Data.Char hiding (isSpace, toUpper)
import Data.Ord (comparing, Down(..))
import Prelude hiding (lookup, filter)
import qualified Prelude as P
import Data.Function (on, (&))

lookup :: Int -> [(Int, a)] -> Maybe a
lookup _ [] = Nothing
lookup k ((k', v) : r) = if k == k' then Just v else lookup k r

filter :: (a -> Bool) -> [a] -> [a]
filter p = P.filter (not . p)

q1 :: [Int] -> [Int]
q1 xs = sortBy (comparing Down) (nub xs) \\ [1]

q2 :: Maybe Int -> Int
q2 = M.fromMaybe 0

q3 :: Map.Map Int Char
q3 = Map.fromList [(1, chr 97), (2, intToDigit 3)]

q4 :: [Int] -> [Int]
q4 xs = xs & P.map (+ 1) & filter even

q5 :: Int -> Int -> Bool
q5 = (==) `on` (`mod` 3)
