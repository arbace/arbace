-- an asynchronous exception interrupting a thunk's evaluation: GHC freezes the evaluation (AP_STACK) and the next
-- demand resumes it; hsi reverts the thunk and the next demand starts it again, so an unsafePerformIO side effect
-- inside runs twice (the class :async-revert, review 189's known divergence)
import Control.Concurrent
import Control.Exception
import Data.IORef
import System.IO.Unsafe

main :: IO ()
main = do
  counter <- newIORef (0 :: Int)
  let v = unsafePerformIO (modifyIORef counter (+ 1)) `seq` (sum [1 .. 2000000 :: Int] `seq` 42 :: Int)
  t <- forkIO (evaluate v >> return ())
  threadDelay 2000
  killThread t
  print v
  readIORef counter >>= print
