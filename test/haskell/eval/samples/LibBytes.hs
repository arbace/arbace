-- bytestring: packed strings, builders, hGet, the cbits (memchr, memcmp, memcpy, fps_* in bytestring's cbits)
import qualified Data.ByteString as B
import qualified Data.ByteString.Char8 as C
import qualified Data.ByteString.Lazy as L
import qualified Data.ByteString.Builder as BB
import System.IO
import Data.Word

main :: IO ()
main = do
  let s = C.pack "hello, world; the quick brown fox"
  print (B.length s, B.elemIndex 44 s, C.split ' ' s)
  print (B.foldl' (\a w -> a + fromIntegral w) (0 :: Int) s)
  print (C.map succ (C.take 5 s), B.reverse (B.take 5 s))
  print (B.maximum s, B.minimum s, B.count 111 s)
  print (C.unpack (C.intercalate (C.pack "--") (C.words s)))
  print (compare s (C.pack "hello"), s == s, B.isPrefixOf (C.pack "hell") s)
  L.putStr (BB.toLazyByteString (BB.intDec 12345 <> BB.char7 ' ' <> BB.doubleDec 3.25 <> BB.char7 '\n' <> BB.word32HexFixed 48879 <> BB.char7 '\n'))
  hSetBinaryMode stdout True
  B.hPut stdout (B.pack [72, 105, 10])
  print (C.readInt (C.pack "12345xyz"), C.readInteger (C.pack "-123456789012345678901234"))
  print (B.sort (B.pack [5,3,9,1,7 :: Word8]))
  print (B.unpack (B.concat [B.pack [1,2], B.pack [3]]))
