-- deadlock found by reachability (GHC's collector): a thread blocked on an MVar no root reaches gets
-- BlockedIndefinitelyOnMVar at performGC or when every thread is blocked; one a root reaches does not; a thread
-- blocked on a TVar no one can write gets BlockedIndefinitelyOnSTM; two threads on each other's thunks NonTermination
import Control.Concurrent
import Control.Concurrent.STM
import Control.Exception
import System.Mem
import Foreign.StablePtr

main :: IO ()
main = do
  out <- newEmptyMVar
  _ <- newStablePtr out -- main, blocked on out, stays reachable while the others are resurrected
  -- unreachable: resurrected at performGC while main sleeps
  _ <- forkIO $ (newEmptyMVar >>= takeMVar) `catch` \e -> putMVar out ("lonely: " ++ show (e :: BlockedIndefinitelyOnMVar))
  threadDelay 10000
  performGC
  takeMVar out >>= putStrLn
  -- reachable through a stable pointer: not resurrected
  kept <- newEmptyMVar
  sp <- newStablePtr kept
  w <- forkIO $ (takeMVar kept >>= putStrLn) `catch` \e -> putStrLn ("kept: " ++ show (e :: BlockedIndefinitelyOnMVar))
  threadDelay 10000
  performGC
  putMVar kept "kept: woken by main"
  threadDelay 10000
  freeStablePtr sp
  -- STM
  _ <- forkIO $ (newTVarIO (0 :: Int) >>= \tv -> atomically (readTVar tv >>= check . (> 0)))
         `catch` \e -> putMVar out ("stm: " ++ show (e :: BlockedIndefinitelyOnSTM))
  takeMVar out >>= putStrLn
  -- a knot of thunks across two threads
  let a = last ([1 .. 10000 :: Int] ++ [b])
      b = last ([2 .. 10000 :: Int] ++ [a])
  _ <- forkIO $ (print a) `catch` \NonTermination -> putMVar out "knot: child NonTermination"
  r <- try (print b)
  print (r :: Either NonTermination ())
  takeMVar out >>= putStrLn
  -- the main thread blocked on an MVar only it reaches
  m <- newEmptyMVar :: IO (MVar ())
  r2 <- try (takeMVar m)
  print (r2 :: Either BlockedIndefinitelyOnMVar ())
