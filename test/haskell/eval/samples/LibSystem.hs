-- directory, filepath, process, time, unix: the cbits of directory, unix, process and time
import System.Directory
import System.FilePath
import System.Process
import System.Exit
import System.IO
import System.Environment
import Control.Exception
import Control.Monad
import System.IO.Error (isDoesNotExistError)
import Data.Time.Clock
import Data.Time.Calendar
import Data.Time.Format
import System.Posix.Files
import System.Posix.Process (getProcessID)
import System.Posix.Types
import System.CPUTime
import System.Timeout
import Control.Concurrent (threadDelay)

main :: IO ()
main = do
  tmp <- getTemporaryDirectory
  let d = tmp </> "hsi-libsystem-test"
  removePathForcibly d
  createDirectoryIfMissing True (d </> "a" </> "b")
  writeFile (d </> "a" </> "f.txt") "hello\n"
  appendFile (d </> "a" </> "f.txt") "world\n"
  listDirectory (d </> "a") >>= print
  doesFileExist (d </> "a" </> "f.txt") >>= print
  getFileSize (d </> "a" </> "f.txt") >>= print
  renameFile (d </> "a" </> "f.txt") (d </> "g.txt")
  copyFile (d </> "g.txt") (d </> "h.txt")
  readFile (d </> "h.txt") >>= putStr
  st <- getFileStatus (d </> "h.txt")
  print (fileSize st, isRegularFile st, isDirectory st, fileMode st `intersectFileModes` 0o777 == 0o644)
  setFileMode (d </> "h.txt") 0o600
  getPermissions (d </> "h.txt") >>= print
  r <- try (readFile (d </> "nonexistent")) :: IO (Either IOException String)
  putStrLn (either (\e -> "Left " ++ show (isDoesNotExistError e)) id r)
  removePathForcibly d
  doesDirectoryExist d >>= print
  print (takeExtension "a/b/c.tar.gz", takeBaseName "a/b/c.tar.gz", splitPath "/x/y/z", "a" </> "b" <.> "c")
  let day = fromGregorian 2026 11 5
  print (day, addDays 100 day, diffDays (fromGregorian 2027 1 1) day, toGregorian day)
  putStrLn (formatTime defaultTimeLocale "%Y-%m-%d %A %j" (UTCTime day 3600))
  t0 <- getCurrentTime
  t1 <- getCurrentTime
  print (diffUTCTime t1 t0 >= 0)
  c <- getCPUTime
  print (c >= 0)
  (ec, out, err) <- readProcessWithExitCode "echo" ["from", "process"] ""
  print (ec, out, err)
  readProcess "tr" ["a-z", "A-Z"] "piped input\n" >>= putStr
  ec2 <- system "exit 3"
  print ec2
  pid <- getProcessID
  print (pid > 0)
  timeout 100000 (return 5) >>= print
  timeout 50000 (threadDelay 5000000) >>= print
  lookupEnv "HSI_NO_SUCH" >>= print
