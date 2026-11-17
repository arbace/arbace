module T10Text where

import Data.Char (toUpper, isDigit)
import Data.List (sortBy, nub)

capitalize [] = []
capitalize (c : cs) = toUpper c : cs

digits s = filter isDigit s

uniqSorted xs = nub (sortBy compare xs)

wordsLen s = map length (words s)

joinWith sep [] = ""
joinWith sep [w] = w
joinWith sep (w : ws) = w ++ sep ++ joinWith sep ws

readInts :: String -> [Int]
readInts s = map read (words s)

main :: IO ()
main = do
  line <- return "1 2 3"
  let ns = readInts line
  print (sum ns)
  putStrLn (joinWith ", " (map show ns))
  mapM_ (putStrLn . capitalize) ["ab", "cd"]
