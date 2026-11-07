-- throwTo to a masked thread blocks the thrower until the target accepts it (HES41 (a)): at its unmasking, or when
-- it blocks interruptibly while masked interruptibly; uninterruptibly masked, never until it unmasks
import Control.Concurrent
import Control.Exception

main :: IO ()
main = do
  ready <- newEmptyMVar
  go <- newEmptyMVar
  done <- newEmptyMVar
  t <- forkIO $ (mask_ $ do
          putMVar ready ()
          takeMVar go
          putStrLn "child: still masked"
          ) `catch` (\e -> putStrLn ("child: caught " ++ show (e :: AsyncException))) >> putMVar done ()
  takeMVar ready
  thrower <- forkIO $ do
    throwTo t ThreadKilled
    putStrLn "thrower: delivered"
  yield
  putStrLn "main: thrower blocked"
  putMVar go ()
  takeMVar done
  yield
  -- interruptibly masked and blocked: accepted at once
  m <- newEmptyMVar
  t2 <- forkIO $ mask_ (takeMVar m) `catch` (\e -> putStrLn ("t2: caught " ++ show (e :: AsyncException)))
  yield
  killThread t2
  putStrLn "main: killed t2"
  -- uninterruptibly masked: the thrower waits for the unmasking
  r <- newEmptyMVar
  t3 <- forkIO $ (uninterruptibleMask_ (threadDelay 20000 >> putStrLn "t3: unmasking") >> threadDelay 100000)
          `catch` (\e -> putStrLn ("t3: caught " ++ show (e :: AsyncException))) >> putMVar r ()
  yield
  killThread t3
  putStrLn "main: killed t3"
  takeMVar r
  -- a thrower blocked in throwTo is itself interruptible
  t4 <- forkIO $ uninterruptibleMask_ (threadDelay 50000)
  t5 <- forkIO $ (throwTo t4 ThreadKilled >> putStrLn "t5: delivered")
          `catch` (\e -> putStrLn ("t5: caught " ++ show (e :: AsyncException)))
  yield
  killThread t5
  threadDelay 100000
  putStrLn "main: end"
