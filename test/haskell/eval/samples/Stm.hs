-- STM: atomically, retry, orElse
import Control.Concurrent
import Control.Concurrent.STM
import Control.Monad

main :: IO ()
main = do
  tv <- newTVarIO (0 :: Int)
  done <- newEmptyMVar
  forM_ [1 .. 10 :: Int] $ \i -> forkIO (atomically (modifyTVar' tv (+ i)) >> putMVar done ())
  replicateM_ 10 (takeMVar done)
  readTVarIO tv >>= print
  gate <- newTVarIO False
  _ <- forkIO (threadDelay 1000 >> atomically (writeTVar gate True))
  atomically (readTVar gate >>= check)
  putStrLn "gate open"
  r <- atomically ((readTVar gate >>= \g -> if g then retry else return "left") `orElse` return "right")
  putStrLn r
