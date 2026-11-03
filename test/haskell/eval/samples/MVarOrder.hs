-- MVars: a reader blocked on an empty MVar goes before the takers (review 189, F3); a thread interrupted in putMVar
-- leaves no put behind (F4); a queued exception delivered when its masked target blocks interruptibly (F5)
import Control.Concurrent
import Control.Exception

main :: IO ()
main = do
  mv <- newEmptyMVar
  out <- newEmptyMVar
  _ <- forkIO $ takeMVar mv >>= \v -> putMVar out ("take " ++ v)
  yield
  _ <- forkIO $ readMVar mv >>= \v -> putMVar out ("read " ++ v)
  yield
  putMVar mv "a"
  takeMVar out >>= putStrLn
  takeMVar out >>= putStrLn
  -- an interrupted putMVar
  full <- newMVar ()
  other <- newEmptyMVar
  res <- newEmptyMVar
  t <- forkIO $ do
    putMVar full () `catch` \e -> putStrLn ("caught " ++ show (e :: AsyncException))
    takeMVar other >>= putMVar res
  yield
  killThread t
  yield
  putMVar other "hello"
  takeMVar res >>= putStrLn
  -- killed while masked and running (the throw queued, its thrower blocked), then blocking interruptibly
  never <- newEmptyMVar :: IO (MVar ())
  r <- newEmptyMVar
  t2 <- forkIO $ mask_ $ do
    yield
    x <- try (takeMVar never)
    putMVar r (either (\e -> show (e :: AsyncException)) (const "taken") x)
  _ <- forkIO (killThread t2 >> putStrLn "killer: delivered")
  takeMVar r >>= putStrLn
  yield
