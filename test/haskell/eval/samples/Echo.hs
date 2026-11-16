-- text I/O: stdin by lines, encodings, arguments and the program's name, the exit status
import System.Environment
import System.Exit
import System.IO
import Data.Char (toUpper)

main :: IO ()
main = do
  name <- getProgName
  args <- getArgs
  hPutStrLn stderr (name ++ " " ++ show args)
  s <- getContents
  let ls = lines s
  mapM_ (putStrLn . map toUpper) ls
  putStrLn ("λ → " ++ show (length ls) ++ " lines")
  exitWith (ExitFailure 3)
