-- time zones, pipes, signals, handles' buffers, process groups, resource limits and the system's names
import System.Posix.IO
import System.Posix.Signals
import System.Posix.Process
import System.Posix.Resource
import System.Posix.Unistd
import System.Posix.Types
import System.IO
import System.Info
import System.Mem
import System.Mem.StableName
import System.Mem.Weak
import Control.Concurrent
import Control.Concurrent.MVar
import Control.Exception
import Control.Monad
import Data.Time
import Data.Unique
import Foreign.Marshal.Alloc
import Foreign.Marshal.Array
import Foreign.Ptr
import Foreign.Storable
import Data.Word (Word8)
import Data.IORef
import Numeric (showFFloat, showHex)
import Text.Printf

main :: IO ()
main = do
  hSetBuffering stdout LineBuffering
  tz <- getCurrentTimeZone
  print (timeZoneName tz, timeZoneMinutes tz)
  zt <- utcToLocalZonedTime (UTCTime (fromGregorian 2026 11 5) 43200)
  print zt
  (rfd, wfd) <- createPipe
  _ <- fdWrite wfd "pipe data"
  closeFd wfd
  (s, n) <- fdRead rfd 64
  print (s, n)
  closeFd rfd
  mv <- newEmptyMVar
  _ <- installHandler sigUSR1 (Catch (putMVar mv "got USR1")) Nothing
  raiseSignal sigUSR1
  takeMVar mv >>= putStrLn
  allocaBytes 16 $ \p -> do
    pokeArray p [65 .. 80 :: Word8]
    hPutBuf stdout p 4
    hFlush stdout
    putStrLn ""
  print =<< (getResourceLimit ResourceCoreFileSize >>= \l -> return (softLimit l == softLimit l))
  print (os, arch, compilerName)
  performGC
  r <- newIORef (0 :: Int)
  sn1 <- makeStableName r
  sn2 <- makeStableName r
  print (sn1 == sn2)
  w <- mkWeakIORef r (return ())
  deRefWeak w >>= \x -> print (fmap (const ()) x)
  u1 <- newUnique
  u2 <- newUnique
  print (u1 /= u2, hashUnique u1 < hashUnique u2)
  putStrLn (showFFloat (Just 3) (3.14159 :: Double) "" ++ " " ++ showHex (255 :: Int) "")
  printf "%5.2f|%-6s|%04d|%x\n" (2.71828 :: Double) "ab" (42 :: Int) (255 :: Int)
  t0 <- getCurrentTime
  threadDelay 10000
  t1 <- getCurrentTime
  print (diffUTCTime t1 t0 >= 0.01)
  usleep 1000
  getProcessGroupID >>= \g -> print (g > 0)
