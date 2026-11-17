-- exceptions in pure and IO code, bracket, finally, masks, throwTo, an uncaught exception at the end
import Control.Exception
import Control.Concurrent
import System.IO
import qualified Data.Map as M

data Boom = Boom Int deriving Show
instance Exception Boom

main :: IO ()
main = do
  hSetBuffering stdout LineBuffering
  r1 <- try (evaluate (1 `div` (0 :: Int)))
  print (r1 :: Either ArithException Int)
  r2 <- try (throwIO (Boom 7))
  print (r2 :: Either Boom ())
  (bracket (putStrLn "acquire" >> return 5) (\_ -> putStrLn "release") (\x -> print (x * 2)))
  (putStrLn "body" >> throwIO (Boom 1)) `catch` (\(Boom n) -> putStrLn ("caught " ++ show n)) `finally` putStrLn "finally"
  st <- mask_ getMaskingState
  print st
  mv <- newEmptyMVar
  t <- forkIO ((threadDelay 1000000 >> putStrLn "not reached") `catch` (\e -> putMVar mv (show (e :: AsyncException))))
  threadDelay 1000
  killThread t
  takeMVar mv >>= putStrLn
  print (M.fromList [(1 :: Int, 'x')] M.! 2)
