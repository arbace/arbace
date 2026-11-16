import qualified Data.Map as M
import Debug.Trace
main :: IO ()
main = do
  let xs = [trace "a" 1, trace "b" 2] :: [Int]
  print (sum (reverse xs))
  print (M.fromList [(1::Int,'x')] M.! 2)
