{-# LANGUAGE MagicHash, UnboxedTuples #-}
-- delimited continuations (prompt#, control0#): a captured continuation resumed twice, the prompt removed by the
-- capture, the masking state across a capture and a resume, and control0# without its prompt
import GHC.Exts
import GHC.IO
import Control.Exception

data Tag a = Tag (PromptTag# a)

newTag :: IO (Tag a)
newTag = IO $ \s -> case newPromptTag# s of (# s', t #) -> (# s', Tag t #)

prompt :: Tag a -> IO a -> IO a
prompt (Tag t) (IO m) = IO (prompt# t m)

control0 :: Tag a -> ((IO b -> IO a) -> IO a) -> IO b
control0 (Tag t) f = IO (control0# t (\k -> case f (\(IO b) -> IO (k b)) of IO a -> a))

main :: IO ()
main = do
  t <- newTag
  r <- prompt t $ do
    x <- control0 t $ \k -> do
      a <- k (return 10)
      b <- k (return 20)
      return (a + b)
    putStrLn ("body with " ++ show x)
    return (x * 2 :: Int)
  print r
  -- masking: captured inside mask_, resumed unmasked; the capture restores the prompt's state
  t2 <- newTag
  s <- prompt t2 $ mask_ $ do
    m1 <- getMaskingState
    v <- control0 t2 $ \k -> do
      m2 <- getMaskingState
      inner <- k (getMaskingState >>= \m -> return (show m))
      return (show m2 ++ " / " ++ inner)
    return (show m1 ++ " -> " ++ v)
  putStrLn s
  -- no prompt
  t3 <- newTag :: IO (Tag ())
  e <- try (control0 t3 (\_ -> return ()))
  case e of
    Left x -> putStrLn ("no prompt: " ++ show (x :: SomeException))
    Right () -> putStrLn "no prompt: returned"
