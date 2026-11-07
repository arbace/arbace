-- weak pointers and finalizers: a dead key's finalizer run after performGC by GHC's finalizer thread (the newest
-- first), a live key's not, finalize run once, deRefWeak of a dead weak pointer Nothing, mkWeakIORef, a C finalizer
import Control.Concurrent
import Control.Monad
import Data.IORef
import System.Mem
import System.Mem.Weak
import Foreign.Marshal.Alloc
import Foreign.ForeignPtr

main :: IO ()
main = do
  done <- newEmptyMVar
  ws <- forM [1 .. 3 :: Int] $ \i -> do
    r <- newIORef i
    mkWeakIORef r (putStrLn ("finalized " ++ show i) >> putMVar done ())
  keep <- newIORef (99 :: Int)
  wk <- mkWeakIORef keep (putStrLn "finalized keep (wrong)")
  performGC
  replicateM_ 3 (takeMVar done)
  forM_ ws $ \w -> deRefWeak w >>= putStrLn . maybe "dead" (const "alive")
  deRefWeak wk >>= \m -> case m of
    Nothing -> putStrLn "keep dead (wrong)"
    Just r -> readIORef r >>= print
  -- finalize: run now, once
  r <- newIORef 'x'
  w <- mkWeakIORef r (putStrLn "finalize ran")
  finalize w
  finalize w
  deRefWeak w >>= putStrLn . maybe "after finalize: dead" (const "after finalize: alive")
  readIORef r >>= print
  -- a ForeignPtr with a C finalizer (free) and one with a Haskell finalizer
  p <- mallocBytes 16
  fp <- newForeignPtr finalizerFree p
  withForeignPtr fp (\_ -> return ())
  q <- mallocBytes 16
  fq <- newForeignPtr_ q
  addForeignPtrFinalizer finalizerFree fq
  performGC
  threadDelay 10000
  readIORef keep >>= print
  putStrLn "end"
