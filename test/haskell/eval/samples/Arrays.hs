-- ST, STRef, unboxed and boxed arrays, IORef, Integer at every size, Double printing
import Control.Monad.ST
import Data.STRef
import Data.Array.IArray (Array)
import Data.Array.ST
import Data.Array.Unboxed
import Data.Bits
import Data.Word
import Data.Int

sieve :: Int -> UArray Int Bool
sieve n = runSTUArray $ do
  a <- newArray (2, n) True
  let loop i | i * i > n = return ()
             | otherwise = do
                 p <- readArray a i
                 if p then mapM_ (\j -> writeArray a j False) [i * i, i * i + i .. n] else return ()
                 loop (i + 1)
  loop 2
  return a

sumST :: [Int] -> Int
sumST xs = runST $ do
  r <- newSTRef 0
  mapM_ (\x -> modifySTRef' r (+ x)) xs
  readSTRef r

main :: IO ()
main = do
  print [i | (i, True) <- assocs (sieve 100)]
  print (sumST [1 .. 1000])
  let a = listArray (0, 4) "hello" :: Array Int Char
  print (a ! 1, bounds a)
  print (product [1 .. 30 :: Integer], 2 ^ (100 :: Int) `div` 3 :: Integer, gcd (2 ^ 64) (3 * 2 ^ 70 :: Integer))
  print (maxBound :: Word8, minBound :: Int64, popCount (255 :: Word16), shiftL (1 :: Int32) 31, complement (0 :: Word))
  print (0.1 + 0.2 :: Double, 1 / 0 :: Double, sqrt 2 :: Float, fromIntegral (3 :: Int) / 7 :: Double, -0.0 :: Double)
  print (read "[(1,\"a\"),(2,\"b\")]" :: [(Int, String)])
