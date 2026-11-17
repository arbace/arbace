-- STM: a transaction that would block on a thunk another thread evaluates aborts and reruns (review 189, F2), so no
-- thread sees its writes; nested atomically refused (F8); catchSTM does not catch an asynchronous exception (F9)
import Control.Concurrent
import Control.Concurrent.STM
import Control.Exception
import GHC.Conc (unsafeIOToSTM)

slow :: Int -> Int
slow n = sum [1 .. n]

main :: IO ()
main = do
  tv <- newTVarIO (0 :: Int)
  let shared = slow 3000000
  started <- newEmptyMVar
  doneB <- newEmptyMVar
  _ <- forkIO $ putMVar started () >> evaluate shared >> putMVar doneB ()
  takeMVar started
  doneA <- newEmptyMVar
  _ <- forkIO $ do
    r <- try $ atomically $ do
      writeTVar tv 1
      _ <- return $! shared
      throwSTM (ErrorCall "abort")
    putStrLn ("A: " ++ either (\e -> show (e :: ErrorCall)) (const "committed") r)
    putMVar doneA ()
  yield
  atomically $ readTVar tv >>= writeTVar tv . (+ 10)
  takeMVar doneA
  takeMVar doneB
  readTVarIO tv >>= print
  -- nested atomically
  r <- try (atomically (unsafeIOToSTM (atomically (writeTVar tv 5))))
  putStrLn (either (\e -> show (e :: SomeException)) (const "nested: ran") r)
  -- an asynchronous exception passes catchSTM
  m <- newEmptyMVar
  t <- forkIO $ do
    r2 <- try $ atomically $ (unsafeIOToSTM (threadDelay 50000) >> return "done")
            `catchSTM` \e -> return ("caught " ++ show (e :: SomeException))
    putMVar m (either (\e -> "Left " ++ show (e :: AsyncException)) ("Right " ++) r2)
  threadDelay 10000
  killThread t
  takeMVar m >>= putStrLn
