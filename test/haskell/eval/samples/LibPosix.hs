-- unix and directory beyond the basics: links, times, environment, users, the working directory, file handles' seeks
import System.Directory hiding (createDirectory, isSymbolicLink)
import System.FilePath
import System.Environment (lookupEnv)
import System.IO
import System.Posix.Files
import System.Posix.IO
import System.Posix.Directory (openDirStream, readDirStream, closeDirStream)
import System.Posix.Env (getEnvironment, setEnv, unsetEnv)
import System.Posix.User
import qualified System.Posix.User
import System.Posix.Time (epochTime)
import System.Posix.Types (UserID)
import System.Posix.Unistd
import Control.Exception
import Control.Monad
import Data.List (sort)
import Data.Time.Clock.POSIX
import Foreign.C.String
import Foreign.Marshal.Alloc
import Foreign.Ptr (castPtr)

main :: IO ()
main = do
  tmp <- getTemporaryDirectory
  let d = tmp </> "hsi-libposix-test"
  removePathForcibly d
  createDirectoryIfMissing False d
  setCurrentDirectory d
  cwd <- getCurrentDirectory
  print (takeFileName cwd)
  writeFile "a.txt" "0123456789"
  createSymbolicLink "a.txt" "l.txt"
  createLink "a.txt" "h.txt"
  readSymbolicLink "l.txt" >>= putStrLn
  System.Posix.Files.isSymbolicLink <$> getSymbolicLinkStatus "l.txt" >>= print
  linkCount <$> getFileStatus "a.txt" >>= print
  readFile "l.txt" >>= putStrLn
  sort <$> listDirectory "." >>= print
  setFileTimes "a.txt" 1000000000 1100000000
  st <- getFileStatus "a.txt"
  print (accessTime st, modificationTime st)
  getModificationTime "a.txt" >>= print
  setPermissions "a.txt" (setOwnerExecutable True emptyPermissions { readable = True })
  fileMode <$> getFileStatus "a.txt" >>= print
  canonicalizePath "l.txt" >>= putStrLn . takeFileName
  doesPathExist "l.txt" >>= print
  pathIsSymbolicLink "l.txt" >>= print
  h <- openFile "a.txt" ReadWriteMode
  hFileSize h >>= print
  hSeek h AbsoluteSeek 4
  hGetChar h >>= print
  hTell h >>= print
  hSeek h SeekFromEnd (-2)
  hGetLine h >>= putStrLn
  hSetFileSize h 5
  hFileSize h >>= print
  hClose h
  fd <- openFd "a.txt" ReadOnly defaultFileFlags
  fdSeek fd AbsoluteSeek 3 >>= print
  allocaBytes 4 $ \p -> do
    n <- fdReadBuf fd p 4
    peekCStringLen (castPtr p, fromIntegral n) >>= putStrLn
  closeFd fd
  setEnv "HSI_LIBPOSIX" "yes" True
  lookupEnv "HSI_LIBPOSIX" >>= print
  (lookup "HSI_LIBPOSIX" <$> getEnvironment) >>= print
  unsetEnv "HSI_LIBPOSIX"
  lookupEnv "HSI_LIBPOSIX" >>= print
  uid <- getRealUserID
  u <- getUserEntryForID uid
  print (userID u == uid)
  r <- try (userID <$> getUserEntryForName "no-such-user-hsi") :: IO (Either IOException UserID)
  putStrLn (either (const "no such user") (const "found") r)
  t <- epochTime
  print (t > 1700000000)
  now <- getPOSIXTime
  print (now > 1700000000)
  ds <- openDirStream "."
  let loop acc = do { e <- readDirStream ds; if null e then return acc else loop (e : acc) }
  loop [] >>= print . sort
  closeDirStream ds
  removeFile "h.txt"
  removeLink "l.txt"
  setCurrentDirectory tmp
  removePathForcibly d
