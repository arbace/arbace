-- the handle layer over files: encodings, binary, buffers, temporary files, seeking, terminals, end of file
import System.IO
import System.IO.Error
import System.Directory
import System.FilePath
import Control.Exception
import Control.Monad
import qualified Data.Text as T
import qualified Data.Text.IO as TIO
import qualified Data.ByteString as B
import qualified Data.ByteString.Char8 as C

main :: IO ()
main = do
  tmp <- getTemporaryDirectory
  (p, h) <- openTempFile tmp "hsi-io.txt"
  hSetEncoding h utf8
  hPutStrLn h "héllo wörld ✓"
  hClose h
  s <- readFile p
  putStrLn s
  print (length s)
  withBinaryFile p ReadMode $ \b -> B.hGetContents b >>= print . B.length
  withFile p ReadMode $ \l -> do
    hSetEncoding l latin1
    hGetContents l >>= print . length
  withFile p WriteMode $ \u -> do
    hSetEncoding u utf16le
    hPutStr u "ab"
  B.readFile p >>= print . B.unpack
  withFile p WriteMode $ \u -> do
    hSetEncoding u utf32be
    hPutStr u "z"
  B.readFile p >>= print . B.unpack
  TIO.writeFile p (T.pack "text\nfile\n")
  TIO.readFile p >>= print
  withFile p ReadMode $ \l -> do
    hIsEOF l >>= print
    a <- hGetLine l
    b <- hGetLine l
    e <- hIsEOF l
    r <- try (hGetLine l)
    print (a, b, e, either (\x -> isEOFError x) (const False) r)
  withFile p ReadWriteMode $ \l -> do
    hSetBuffering l (BlockBuffering (Just 4))
    hPutStr l "XY"
    hSeek l AbsoluteSeek 0
    hGetChar l >>= print
    hGetBuffering l >>= print
    hLookAhead l >>= print
    hIsSeekable l >>= print
    hIsOpen l >>= print
  hIsTerminalDevice stdin >>= print
  hIsTerminalDevice stdout >>= print
  hGetEncoding stdout >>= print
  hGetBuffering stdout >>= print
  r <- try (openFile (tmp </> "no" </> "such") ReadMode) :: IO (Either IOException Handle)
  putStrLn (either (\e -> show (isDoesNotExistError e)) (const "opened") r)
  r2 <- try (openFile tmp WriteMode) :: IO (Either IOException Handle)
  putStrLn (either (\e -> show (ioeGetErrorType e)) (const "opened") r2)
  removeFile p
  hPutStrLn stderr "to stderr"
  hFlush stdout
