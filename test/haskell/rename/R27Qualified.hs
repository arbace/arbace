module R27Qualified where

import qualified Data.Map as M
import qualified Data.List as L
import Data.Maybe (mapMaybe, catMaybes)
import qualified Data.Char

m1 :: M.Map String Int
m1 = M.insert "a" 1 M.empty

m2 :: Maybe Int
m2 = M.lookup "a" m1

m3 :: [Int]
m3 = L.sort (M.elems m1) L.++ L.reverse [1, 2, 3]

m4 :: String
m4 = map Data.Char.toUpper "abc"

m5 :: [Int]
m5 = mapMaybe (`M.lookup` m1) ["a"] ++ catMaybes [Just 1]

m6 :: M.Map String Int -> [(String, Int)]
m6 = M.toList . M.filter (> 0)
