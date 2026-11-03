-- green threads, MVars, threadDelay, a deadlock detected (BlockedIndefinitelyOnMVar)
import Control.Concurrent
import Control.Exception
import Control.Monad

main :: IO ()
main = do
  out <- newMVar ()
  done <- newEmptyMVar
  forM_ [1 .. 3 :: Int] $ \i -> forkIO $ do
    threadDelay (i * 2000)
    withMVar out (\_ -> putStrLn ("thread " ++ show i))
    putMVar done i
  rs <- replicateM 3 (takeMVar done)
  print rs
  chan <- newChan
  _ <- forkIO (mapM_ (writeChan chan) [1 .. 5 :: Int])
  xs <- replicateM 5 (readChan chan)
  print (sum xs)
  never <- newEmptyMVar :: IO (MVar ())
  r <- try (takeMVar never)
  case r of
    Left BlockedIndefinitelyOnMVar -> putStrLn "deadlock detected"
    Right () -> putStrLn "impossible"
