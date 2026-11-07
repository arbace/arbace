-- containers, array, deepseq, transformers, mtl, stm
import qualified Data.Map.Strict as M
import qualified Data.Set as S
import qualified Data.IntMap as IM
import qualified Data.Sequence as Q
import Data.Array
import Control.Monad.State
import Control.Concurrent.STM
import Control.DeepSeq
import Data.Tree
import Data.Foldable (toList)

main :: IO ()
main = do
  let m = M.fromListWith (+) [(w, 1 :: Int) | w <- words "a b a c b a d e f a"]
  print (M.toList m, M.lookupMin m, M.foldr (+) 0 m)
  print (S.toList (S.map (`mod` 7) (S.fromList [1 .. 50 :: Int])))
  print (IM.toList (IM.fromList [(i * 7 `mod` 11, i) | i <- [1 .. 10]]))
  print (toList (Q.reverse (Q.fromList [1 .. 10 :: Int])))
  let a = listArray (0, 9) [i * i | i <- [0 .. 9]] :: Array Int Int
  print (a ! 5, bounds a, sum (elems a), accumArray (+) 0 (0, 2) [(i `mod` 3, i) | i <- [1 .. 10]] :: Array Int Int)
  print (execState (mapM_ (\x -> modify (+ x)) [1 .. 100]) (0 :: Int))
  tv <- newTVarIO (0 :: Int)
  atomically (mapM_ (\x -> modifyTVar' tv (+ x)) [1 .. 10])
  readTVarIO tv >>= print
  print (force (map (* 2) [1 .. 5 :: Int]) `deepseq` "forced")
  putStr (drawTree (fmap show (Node 1 [Node 2 [Node 4 []], Node 3 []])))
