-- text: Data.Text's cbits and simdutf-free paths (UTF-8 validate, measure, memcpy), Text.IO, encode/decode
import qualified Data.Text as T
import qualified Data.Text.IO as TIO
import qualified Data.Text.Encoding as TE
import qualified Data.Text.Lazy as TL
import qualified Data.Text.Lazy.Builder as TB
import qualified Data.ByteString as B

main :: IO ()
main = do
  let t = T.pack "Grüße, Jürgen — λ→ 日本語 text"
  TIO.putStrLn t
  print (T.length t, T.toUpper t, T.words t)
  print (B.length (TE.encodeUtf8 t), TE.decodeUtf8 (TE.encodeUtf8 t) == t)
  print (T.splitOn (T.pack ", ") t, T.replace (T.pack "text") (T.pack "TEXT") t)
  print (T.breakOn (T.pack "—") t, T.isInfixOf (T.pack "日本") t)
  print (TL.toStrict (TB.toLazyText (TB.fromText t <> TB.fromString "!" <> TB.fromString (show (42 :: Int)))))
  print (TE.decodeUtf8' (B.pack [0xff, 0x41]))
  print (T.unpack (T.reverse t), T.center 40 '*' (T.pack "mid"))
