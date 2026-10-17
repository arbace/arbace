{-# LANGUAGE CPP, BangPatterns, ScopedTypeVariables, RankNTypes, LambdaCase, TypeApplications, GADTs #-}
-- repr/haskell's helper (doc/haskell/SPEC.md §8, doc/haskell/HASKELL-NOTES.md): GHC's parsed tree
-- of a Haskell module (HsModule GhcPs, after GHC's driver preprocessed it: unlit, CPP) written
-- generically through Data as Clojure-reader forms, for the converter arbace.repr.haskell.convert.
-- It decides nothing about forms. Arbace's own code, written anew; GHC's utils/check-ppr (its
-- eraseEpLayout and the showAstData comparison) is its model (SPEC HS8).
--
-- Usage (bin/repr-haskell runs it):
--   hsdump dump  [--full] -o OUTDIR [--libdir DIR] [--corpus NAME] [--ghc FLAG... --files] FILE...
--       one OUTDIR/FILE.edn per module: (hsdump/config {...}) (hsdump/pragmas [...]) then the
--       tree (SPEC §8.2); with --full also the exact-print annotations (R3, not yet used)
--   hsdump tree  [--full] -o OUTDIR [--libdir DIR] [--ghc FLAG... --files] FILE...
--       one OUTDIR/FILE.tree per module: the tree level's normal form (SPEC §9.2): showAstData
--       BlankSrcSpan BlankEpAnnotations after eraseEpLayout (NoBlank... with --full), and
--       OUTDIR/FILE.lines: the start line of every located node (hsdump lines)
--   hsdump lines [--libdir DIR] [--ghc FLAG... --files] FILE...   the lines, to standard output
-- FILE is relative to the current directory (the package's directory: it is the file name of
-- every span). The GHC flags between --ghc and --files are the conversion's configuration (SPEC §2.7):
-- -X (the language and extensions), -D and -I (CPP), -haddock, -this-unit-id, -package...
-- The exit status is 1 when a module fails (its output is (hsdump/error "...")).
module Main (main) where

import GHC
import GHC.Driver.Session
import GHC.Driver.Env (HscEnv(..), hscSetFlags)
import GHC.Driver.Config.Parser (initParserOpts, supportedLanguagePragmas)
import GHC.Driver.Pipeline (preprocess)
import GHC.Driver.Errors.Types (DriverMessages, DriverMessage)
import GHC.Parser.Errors.Types (PsMessage)
import GHC.Utils.Logger (LogAction, Logger, pushLogHook, setLogFlags)
import GHC.Driver.Config.Logger (initLogFlags)
import GHC.Parser.Lexer (ParserOpts, initParserState, initPragState, unP, lexer, ParseResult(..), Token(..), getPsErrorMessages)
import qualified GHC.Parser as P
import GHC.Parser.Header (toArgs)
import GHC.Data.StringBuffer (StringBuffer, hGetStringBuffer, lexemeToString, atLine, stringToStringBuffer)
import GHC.Data.FastString
import qualified GHC.Data.Strict
import GHC.Types.SrcLoc
import GHC.Types.SourceText (SourceText(..))
import GHC.Types.Name.Reader (RdrName(..))
import GHC.Types.Name.Occurrence
import GHC.Types.Name (getOccString)
import GHC.Unit.Types (moduleUnit, moduleName, toUnitId, unitIdString)
import GHC.Unit.Module (unitString)
import GHC.Hs.Dump (showAstData, BlankSrcSpan(..), BlankEpAnnotations(..))
import GHC.Hs.DocString (HsDocStringChunk, unpackHDSC)
import GHC.Utils.Outputable (showSDocUnsafe, renderWithContext, defaultSDocContext)
import GHC.Utils.Error (pprMessages)
import GHC.Types.Error (getMessages, MsgEnvelope(..), defaultDiagnosticOpts)
import GHC.Settings.Config (cProjectVersion)
import GHC.Platform.ArchOS (stringEncodeArch, stringEncodeOS)
import GHC.Platform (platformArchOS, ArchOS(..))
import GHC.Settings (sTargetPlatformString, toolSettings_pgm_P)
import qualified GHC.LanguageExtensions as LangExt

import Control.Exception (SomeException, try, evaluate)
import Control.Monad (forM, forM_, when, unless)
import Control.Monad.IO.Class (liftIO)
import Data.Bits
import Data.Char (isSpace, ord, toLower, toUpper, isDigit)
import Data.Data
import Data.IORef
import Data.List (isPrefixOf, isSuffixOf, sortOn, intercalate, nub)
import qualified Data.List.NonEmpty as NE
import Data.Maybe (fromMaybe, mapMaybe, catMaybes)
import Data.Ratio (numerator, denominator)
import Data.Word (Word8, Word32, Word64)
import qualified Data.ByteString as B
import qualified Data.ByteString.Builder as BB
import qualified Data.ByteString.Char8 as BC
import System.Directory (createDirectoryIfMissing, doesFileExist, listDirectory, makeAbsolute)
import System.Environment (getArgs)
import System.Exit (exitWith, ExitCode(..))
import System.FilePath (takeDirectory, (</>), makeRelative, normalise)
import System.IO

-- The helper's version: bumped when its output changes (recorded in every dump's config).
helperVersion :: Int
helperVersion = 1

-- The GHC this helper was written against (tools/hsdump/GHC_VERSION, passed by the build as
-- -DGHC_PIN); the helper refuses to run on another (SPEC §8.4, H11).
pinnedGhc :: String
#ifdef GHC_PIN
pinnedGhc = GHC_PIN
#else
pinnedGhc = "unset"
#endif

data Opts = Opts
  { oVerb :: String, oFull :: Bool, oOut :: Maybe FilePath, oLibdir :: Maybe FilePath
  , oCorpus :: Maybe String, oSrcHash :: Maybe String, oFiles :: [FilePath], oGhc :: [String] }

parseOpts :: [String] -> Either String Opts
parseOpts (verb : rest) = go (Opts verb False Nothing Nothing Nothing Nothing [] []) rest
  where
    go o ("--ghc" : fs) = let (gs, r) = break (== "--files") fs in go o { oGhc = oGhc o ++ gs } (drop 1 r)
    go o ("--full" : xs) = go o { oFull = True } xs
    go o ("-o" : d : xs) = go o { oOut = Just d } xs
    go o ("--libdir" : d : xs) = go o { oLibdir = Just d } xs
    go o ("--corpus" : c : xs) = go o { oCorpus = Just c } xs
    go o ("--helper-hash" : h : xs) = go o { oSrcHash = Just h } xs
    go o (x : xs) | "-" `isPrefixOf` x = Left ("hsdump: unknown option " ++ x)
                  | otherwise = go o { oFiles = x : oFiles o } xs
    go o [] = Right o { oFiles = reverse (oFiles o) }
parseOpts [] = Left "usage: hsdump dump|tree|lines [OPTIONS] [--ghc FLAG... --files] FILE..."

main :: IO ()
main = do
  hSetEncoding stdout utf8
  args <- getArgs
  o <- either (\e -> hPutStrLn stderr e >> exitWith (ExitFailure 2)) return (parseOpts args)
  when (cProjectVersion /= pinnedGhc) $ do
    hPutStrLn stderr ("hsdump: built with GHC " ++ cProjectVersion ++ ", pinned " ++ pinnedGhc ++ " (tools/hsdump/GHC_VERSION)")
    exitWith (ExitFailure 2)
  libdir <- maybe (fail "hsdump: --libdir DIR (ghc --print-libdir)") return (oLibdir o)
  ghcUnit <- libdirGhcUnit libdir
  case ghcUnit of
    Nothing -> do
      hPutStrLn stderr ("hsdump: " ++ libdir ++ " holds no ghc-" ++ cProjectVersion ++ " unit: not the GHC this helper was built with")
      exitWith (ExitFailure 2)
    Just _ -> return ()
  failures <- newIORef (0 :: Int)
  runGhc (Just libdir) $ do
    dflags0 <- getSessionDynFlags
    logger <- getLogger
    (dflags1, leftover, _) <- parseDynamicFlagsCmdLine logger dflags0 (map noLoc (oGhc o))
    unless (null leftover) $ liftIO $ do
      hPutStrLn stderr ("hsdump: unknown GHC flags: " ++ unwords (map unLoc leftover))
      exitWith (ExitFailure 2)
    let dflags2 = (if oFull o then (`gopt_set` Opt_KeepRawTokenStream) else id) dflags1
    _ <- setSessionDynFlags dflags2
    hsc_env <- getSession
    forM_ (oFiles o) $ \file -> do
      r <- liftIO (try (parseOne hsc_env o file) :: IO (Either SomeException (Either String ModP)))
      let failed msg = liftIO $ do
            modifyIORef' failures (+ 1)
            hPutStrLn stderr ("hsdump: " ++ file ++ ": " ++ firstLine msg)
            return (Left msg)
      res <- case r of
        Left e -> failed (show e)
        Right (Left e) -> failed e
        Right (Right p) -> return (Right p)
      liftIO $ case oVerb o of
        "dump" -> do
          out <- outFile o file ".edn"
          withUtf8File out $ \h -> case res of
            Left msg -> hPutBuilder' h (BB.string7 "(hsdump/error " <> str msg <> BB.char7 ')' <> BB.char7 '\n')
            Right p -> do
              cfg <- configOf o libdir (fromMaybe "" ghcUnit) dflags2 p
              hPutBuilder' h (cfg <> BB.char7 '\n')
              hPutBuilder' h (pragmasOf p <> BB.char7 '\n')
              hPutBuilder' h (gen (oFull o) (mkFastString file) (pModule p) <> BB.char7 '\n')
        "tree" -> do
          out <- outFile o file ".tree"
          outl <- outFile o file ".lines"
          case res of
            Left msg -> do
              withUtf8File out $ \h -> hPutStrLn h ("hsdump/error " ++ firstLine msg)
              withUtf8File outl $ \h -> hPutStrLn h ("hsdump/error " ++ firstLine msg)
            Right p -> do
              withUtf8File out $ \h -> hPutStr h (treeOf (oFull o) (pModule p))
              withUtf8File outl $ \h -> hPutBuilder' h (linesOf (mkFastString file) (pModule p))
        "lines" -> case res of
          Left msg -> putStrLn ("hsdump/error " ++ firstLine msg)
          Right p -> hPutBuilder' stdout (linesOf (mkFastString file) (pModule p))
        v -> hPutStrLn stderr ("hsdump: unknown verb " ++ v) >> exitWith (ExitFailure 2)
  n <- readIORef failures
  when (n > 0) $ exitWith (ExitFailure 1)
  where
    firstLine = unwords . words . map (\c -> if c == '\n' then ' ' else c)

withUtf8File :: FilePath -> (Handle -> IO ()) -> IO ()
withUtf8File f k = withFile f WriteMode (\h -> hSetEncoding h utf8 >> k h)

hPutBuilder' :: Handle -> BB.Builder -> IO ()
hPutBuilder' h b = hSetBinaryMode h True >> BB.hPutBuilder h b

outFile :: Opts -> FilePath -> String -> IO FilePath
outFile o file ext = do
  dir <- maybe (fail "hsdump: -o OUTDIR") return (oOut o)
  let f = dir </> normalise file ++ ext
  createDirectoryIfMissing True (takeDirectory f)
  return f

-- The unit id of the ghc library in LIBDIR's package database: ghc-VERSION-HASH
libdirGhcUnit :: FilePath -> IO (Maybe String)
libdirGhcUnit libdir = do
  fs <- try (listDirectory (libdir </> "package.conf.d")) :: IO (Either SomeException [FilePath])
  let pre = "ghc-" ++ cProjectVersion ++ "-"
  return $ case fs of
    Right xs -> case [take (length x - 5) x | x <- xs, pre `isPrefixOf` x, ".conf" `isSuffixOf` x] of
      (u : _) -> Just u
      [] -> Nothing
    Left _ -> Nothing

-- ---------------------------------------------------------------------------------------
-- Parsing one module

data ModP = ModP
  { pModule :: Located (HsModule GhcPs)
  , pSource :: B.ByteString         -- the original file
  , pText :: B.ByteString           -- the text parsed (the CPP output, or the original)
  , pPragmas :: [(String, Int, Either String [String])]  -- header pragmas: kind, line, value
  , pCpp :: Maybe CppRecord
  , pDflags :: DynFlags }

data CppRecord = CppRecord
  { cCommand :: [String], cArgs :: [String], cFiles :: [(String, String)]
  , cMacros :: Maybe String, cOutput :: String }

parseOne :: HscEnv -> Opts -> FilePath -> IO (Either String ModP)
parseOne hsc_env o file = do
  src <- B.readFile file
  let dflags0 = hsc_dflags hsc_env
  -- the file's own pragmas (before CPP), as GHC's driver reads them
  buf0 <- hGetStringBuffer file
  let popts0 = initParserOpts dflags0
      cppOn0 = xopt LangExt.Cpp dflags0
  (dflagsP, _, _) <- parseDynamicFilePragma (hsc_logger' hsc_env) dflags0 (snd (headerOptions popts0 dflags0 buf0 file))
  if xopt LangExt.Cpp dflagsP || cppOn0
    then do
      -- through GHC's driver, as it preprocesses before parsing (SPEC §2.8, §8.5); the command
      -- line traced at verbosity 3
      traced <- newIORef []
      let dflags3 = dflags0 { verbosity = 3 }
          hsc_env3 = (hscSetFlags dflags3 hsc_env)
            { hsc_logger = pushLogHook (captureHook traced) (setLogFlags (hsc_logger hsc_env) (initLogFlags dflags3)) }
      r <- preprocess hsc_env3 file Nothing Nothing
      case r of
        Left msgs -> return (Left ("preprocessing failed: " ++ showSDocUnsafe (pprMessages (defaultDiagnosticOpts @DriverMessage) msgs)))
        Right (dflagsC, outFn) -> do
          out <- B.readFile outFn
          logged <- readIORef traced
          let pgm = fst (toolSettings_pgm_P (toolSettings dflags0))
              cmdLines = [l | m <- reverse logged, l <- lines m, (pgm ++ " ") `isPrefixOf` l]
              cmdWords = case cmdLines of (l : _) -> shellWords l; [] -> []
              includes = [f | (a, f) <- zip cmdWords (drop 1 cmdWords), a == "-include"]
                ++ [drop 8 a | a <- cmdWords, "-include" `isPrefixOf` a, length a > 8]
              macrosFile = [f | f <- includes, not ("ghcversion.h" `isSuffixOf` f)]
              configured = map showOpt' (snd (toolSettings_pgm_P (toolSettings dflags0)))
          macros <- forM (take 1 macrosFile) $ \f -> do
            e <- doesFileExist f
            if e then Just . sha256Hex <$> B.readFile f else return Nothing
          let markerFiles = nub [f | l <- BC.lines out, Just f <- [lineMarkerFile (BC.unpack l)], f /= file, not ("<" `isPrefixOf` f)]
          fileHashes <- forM markerFiles $ \f -> do
            e <- doesFileExist f
            h <- if e then sha256Hex <$> B.readFile f else return "missing"
            return (if f `elem` macrosFile then "<macros>" else relLibdir (oLibdir o) f, "sha256:" ++ h)
          let record = CppRecord
                { cCommand = pgm : configured
                , cArgs = map (roleOf (oLibdir o) outFn file macrosFile) (dropPrefix configured (drop 1 cmdWords))
                , cFiles = fileHashes
                , cMacros = fmap ("sha256:" ++) (case macros of (Just m : _) -> Just m; _ -> Nothing)
                , cOutput = "sha256:" ++ sha256Hex (foldr (\m t -> replaceBS (BC.pack m) (BC.pack "<macros>") t) out macrosFile) }
          parseText hsc_env dflagsC file src out (Just record)
    else parseText hsc_env dflagsP file src src Nothing
  where
    dropPrefix (p : ps) (x : xs) | p == x = dropPrefix ps xs
    dropPrefix _ xs = xs
    showOpt' (Option s) = s
    showOpt' (FileOption a b) = a ++ b

-- every occurrence of a in t replaced by b
replaceBS :: B.ByteString -> B.ByteString -> B.ByteString -> B.ByteString
replaceBS a b t = case B.breakSubstring a t of
  (x, y) | B.null y -> x
         | otherwise -> B.concat [x, b, replaceBS a b (B.drop (B.length a) y)]

hsc_logger' :: HscEnv -> Logger
hsc_logger' = hsc_logger

captureHook :: IORef [String] -> LogAction -> LogAction
captureHook ref _ _ _ _ doc = modifyIORef' ref (renderWithContext defaultSDocContext doc :)

-- "# 12 \"file\" 2" -> file
lineMarkerFile :: String -> Maybe String
lineMarkerFile ('#' : ' ' : r)
  | (n@(_ : _), ' ' : '"' : q) <- span isDigit r, all isDigit n = Just (unq q)
  where
    unq ('\\' : c : xs) = c : unq xs
    unq ('"' : _) = []
    unq (c : xs) = c : unq xs
    unq [] = []
lineMarkerFile _ = Nothing

-- words of a shell command line as GHC shows it (showCommandForUser quotes with ')
shellWords :: String -> [String]
shellWords s = case dropWhile isSpace s of
  "" -> []
  s' -> let (w, r) = word s' in w : shellWords r
  where
    word ('\'' : r) = let (a, b) = break (== '\'') r in let (c, d) = word (drop 1 b) in (a ++ c, d)
    word ('\\' : c : r) = let (a, b) = word r in (c : a, b)
    word (c : r) | isSpace c = ("", r)
                 | otherwise = let (a, b) = word r in (c : a, b)
    word [] = ("", "")

-- an argument of the CPP command line with its temporary paths replaced by their role
roleOf :: Maybe FilePath -> FilePath -> FilePath -> [FilePath] -> String -> String
roleOf libdir outFn file macros a
  | a == outFn = "<output>"
  | a `elem` macros = "<macros>"
  | "-include" `isPrefixOf` a, drop 8 a `elem` macros = "-include<macros>"
  | ".hscpp" `isSuffixOf` a || "/tmp/" `isPrefixOf` a && ".hspp" `isSuffixOf` a = "<output>"
  | "-I" `isPrefixOf` a = "-I" ++ relLibdir libdir (drop 2 a)
  | otherwise = relLibdir libdir a

relLibdir :: Maybe FilePath -> FilePath -> FilePath
relLibdir (Just d) f | d `isPrefixOf` f = "$libdir" ++ drop (length d) f
relLibdir _ f = f

parseText :: HscEnv -> DynFlags -> FilePath -> B.ByteString -> B.ByteString -> Maybe CppRecord -> IO (Either String ModP)
parseText hsc_env dflagsIn file src text cpp = do
  let buf = bufferOf text
      popts = initParserOpts dflagsIn
      (prags, opts) = headerOptions popts dflagsIn buf file
  (dflags, _, _) <- parseDynamicFilePragma (hsc_logger hsc_env) dflagsIn opts
  let st = initParserState (initParserOpts dflags) buf (mkRealSrcLoc (mkFastString file) 1 1)
  case unP P.parseModule st of
    POk _ m -> return (Right (ModP m src text prags cpp dflags))
    PFailed pst -> return (Left ("parse failed: " ++ showSDocUnsafe (pprMessages (defaultDiagnosticOpts @PsMessage) (getPsErrorMessages pst))))

bufferOf :: B.ByteString -> StringBuffer
bufferOf = stringToStringBuffer . decodeUtf8

decodeUtf8 :: B.ByteString -> String
decodeUtf8 = go . B.unpack
  where
    go [] = []
    go (w : ws)
      | w < 0x80 = toEnum (fromIntegral w) : go ws
      | w .&. 0xE0 == 0xC0, (a : r) <- ws = toEnum (((fromIntegral w .&. 0x1F) `shiftL` 6) .|. cont a) : go r
      | w .&. 0xF0 == 0xE0, (a : b : r) <- ws = toEnum (((fromIntegral w .&. 0x0F) `shiftL` 12) .|. (cont a `shiftL` 6) .|. cont b) : go r
      | w .&. 0xF8 == 0xF0, (a : b : c : r) <- ws = toEnum (((fromIntegral w .&. 0x07) `shiftL` 18) .|. (cont a `shiftL` 12) .|. (cont b `shiftL` 6) .|. cont c) : go r
      | otherwise = '\xFFFD' : go ws
    cont x = fromIntegral x .&. 0x3F

-- The header pragmas (GHC.Parser.Header's getOptions, its loop kept with each pragma's kind and
-- line), and the options GHC derives from them
headerOptions :: ParserOpts -> DynFlags -> StringBuffer -> FilePath -> ([(String, Int, Either String [String])], [Located String])
headerOptions popts dflags buf file = (prags, concatMap optsOf prags')
  where
    toks = lexAll (initPragState popts buf (mkRealSrcLoc (mkFastString file) 1 1))
    lexAll s = case unP (lexer False return) s of
      POk _ t@(L _ ITeof) -> [t]
      POk s' t -> t : lexAll s'
      _ -> []
    prags' = scan toks
    prags = [(k, l, v) | (k, l, v, _) <- prags']
    optsOf (_, _, _, os) = os
    lineOf t = case getLoc t of RealSrcSpan r _ -> srcSpanStartLine r; _ -> 0
    scan (open : close : xs)
      | IToptions_prag s <- unLoc open, ITclose_prag <- unLoc close
      = let args = either (const []) id (toArgs (startOf open) s)
            kind = if "OPTIONS_GHC" `isPrefixOf` map toUpper (pragName open) then "options-ghc" else "options"
        in (kind, lineOf open, Right (map unLoc args), args) : scan xs
      | ITinclude_prag s <- unLoc open, ITclose_prag <- unLoc close
      = ("include", lineOf open, Left s, map (L (getLoc open)) ["-#include", filter (not . isSpace) s]) : scan xs
      | ITdocOptions s _ <- unLoc open, ITclose_prag <- unLoc close
      = ("options-haddock", lineOf open, Left s, map (L (getLoc open)) ["-haddock-opts", filter (not . isSpace) s]) : scan xs
    scan (open : xs)
      | ITlanguage_prag <- unLoc open = lang (lineOf open) [] xs
    scan (c : xs) | isComment (unLoc c) = scan xs
    scan _ = []
    lang l acc (L loc (ITconid fs) : rest) =
      let acc' = unpackFS fs : acc
          o = L loc ("-X" ++ unpackFS fs)
      in case rest of
           L _ ITcomma : more -> lang' l acc' (o :) more
           L _ ITclose_prag : more -> ("language", l, Right (reverse acc'), o : []) `consOpts` scan more
           _ -> []
    lang _ _ _ = []
    lang' l acc k more = case lang l acc more of
      ((kind, l', v, os) : r) -> (kind, l', v, k os) : r
      [] -> []
    consOpts (k, l, v, os) r = (k, l, v, os) : r
    isComment = \case ITlineComment {} -> True; ITblockComment {} -> True; ITdocComment {} -> True; _ -> False
    startOf t = case getLoc t of RealSrcSpan r _ -> realSrcSpanStart r; _ -> mkRealSrcLoc (mkFastString file) 1 1
    -- the pragma's name as the source spells it: {-# OPTIONS_GHC ...
    pragName t = case getLoc t of
      RealSrcSpan r _ ->
        let ls = lines (lexemeToString (fromMaybe buf (atLine (srcSpanStartLine r) buf)) 100000)
            l = case ls of (x : _) -> drop (srcSpanStartCol r - 1) x; [] -> ""
        in takeWhile (\c -> not (isSpace c)) (dropWhile isSpace (drop 3 l))
      _ -> ""

-- ---------------------------------------------------------------------------------------
-- The configuration record (SPEC §2.7, §2.8)

configOf :: Opts -> FilePath -> String -> DynFlags -> ModP -> IO BB.Builder
configOf o libdir ghcUnit dflags0 p = do
  let flags = oGhc o
      xs = [drop 2 f | f <- flags, "-X" `isPrefixOf` f]
      langs = ["Haskell98", "Haskell2010", "GHC2021", "GHC2024"]
      language = case [x | x <- xs, x `elem` langs] of [] -> "GHC2021"; ls -> last ls
      exts = [x | x <- xs, x `notElem` langs]
      defines = [f | f <- flags, "-D" `isPrefixOf` f]
      incs = [drop 2 f | f <- flags, "-I" `isPrefixOf` f]
      unit = case dropWhile (/= "-this-unit-id") flags of (_ : u : _) -> Just u; _ -> Nothing
      other = [f | f <- flags, not (any (`isPrefixOf` f) ["-X", "-D", "-I"])]
      ArchOS arch os = platformArchOS (targetPlatform dflags0)
      kv k v = BB.char7 ' ' <> BB.string7 k <> BB.char7 ' ' <> v
      strs ss = BB.char7 '[' <> mconcat (sepBy (BB.char7 ' ') (map str ss)) <> BB.char7 ']'
      syms ss = BB.char7 '[' <> mconcat (sepBy (BB.char7 ' ') (map BB.stringUtf8 ss)) <> BB.char7 ']'
  let cppB = case pCpp p of
        Nothing -> mempty
        Just c -> kv ":cpp" (BB.char7 '{' <> BB.string7 ":command " <> strs (cCommand c)
                    <> kv ":args" (strs (cArgs c))
                    <> kv ":defines" (strs defines)
                    <> kv ":include-dirs" (strs (map (relLibdir (oLibdir o)) incs))
                    <> kv ":files" (BB.char7 '{' <> mconcat (sepBy (BB.char7 ' ') [str a <> BB.char7 ' ' <> str b | (a, b) <- cFiles c]) <> BB.char7 '}')
                    <> maybe mempty (kv ":macros" . str) (cMacros c)
                    <> kv ":output" (str (cOutput c)) <> BB.char7 '}')
  return $ BB.string7 "(hsdump/config {:ghc " <> str cProjectVersion
    <> kv ":ghc-unit" (str ghcUnit)
    <> kv ":target" (str (sTargetPlatformString (settings dflags0)))
    <> kv ":os" (str (stringEncodeOS os)) <> kv ":arch" (str (stringEncodeArch arch))
    <> kv ":language" (BB.stringUtf8 language)
    <> (if null exts then mempty else kv ":extensions" (syms exts))
    <> kv ":flags" (strs other)
    <> maybe mempty (kv ":unit" . str) unit
    <> maybe mempty (kv ":corpus" . str) (oCorpus o)
    <> kv ":helper" (BB.string7 "{:version " <> BB.intDec helperVersion <> maybe mempty (kv ":source" . str) (oSrcHash o) <> BB.char7 '}')
    <> kv ":positions" (BB.string7 (if oFull o then ":full" else ":lines"))
    <> kv ":source" (str ("sha256:" ++ sha256Hex (pSource p)))
    <> cppB
    <> BB.string7 "})"

pragmasOf :: ModP -> BB.Builder
pragmasOf p = BB.string7 "(hsdump/pragmas [" <> mconcat (sepBy (BB.char7 ' ') (map one (pPragmas p))) <> BB.string7 "])"
  where
    one (k, l, v) = BB.char7 '(' <> BB.string7 k <> BB.char7 ' ' <> BB.intDec l <> BB.char7 ' '
      <> either str (\ws -> BB.char7 '[' <> mconcat (sepBy (BB.char7 ' ') (map str ws)) <> BB.char7 ']') v <> BB.char7 ')'

sepBy :: a -> [a] -> [a]
sepBy _ [] = []
sepBy s (x : xs) = x : concatMap (\y -> [s, y]) xs

-- ---------------------------------------------------------------------------------------
-- The generic dump (SPEC §8.2)

str :: String -> BB.Builder
str s = BB.char7 '"' <> foldMap e s <> BB.char7 '"'
  where
    e '"' = BB.string7 "\\\""
    e '\\' = BB.string7 "\\\\"
    e '\n' = BB.string7 "\\n"
    e '\t' = BB.string7 "\\t"
    e '\r' = BB.string7 "\\r"
    e c | n < 32 || n == 127 || (n >= 0xD800 && n <= 0xDFFF) || (n >= 0x80 && n < 0xA0) = BB.string7 "\\u" <> hex4 n
        | n > 0xFFFF = BB.charUtf8 c
        | otherwise = BB.charUtf8 c
      where n = ord c
    hex4 n = BB.string7 (let h = showHex' n in replicate (4 - length h) '0' ++ h)

showHex' :: Int -> String
showHex' 0 = "0"
showHex' n = reverse (go n) where
  go 0 = ""
  go k = "0123456789abcdef" !! (k `mod` 16) : go (k `div` 16)

-- annotation types: exact-print data, written only with --full (SPEC §8.2)
isAnnType :: String -> Bool
isAnnType tn =
  ("Ep" `isPrefixOf` tn && tn /= "EpAnnHsCase" || "Ann" `isPrefixOf` tn && tn `notElem` ["AnnDecl", "AnnProvenance"])
    || tn `elem` ["NameAnn", "NameAdornment", "TrailingAnn", "IsUnicodeSyntax", "HasE", "BindTag", "DeclTag", "NoEpAnns", "TokenLocation", "EpaLocation'", "EpAnnHsCase"]

tyName :: Data d => d -> String
tyName x = tyConName (typeRepTyCon (typeOf x))

gen :: forall d. Data d => Bool -> FastString -> d -> BB.Builder
gen full modFile = go
  where
    go :: forall e. Data e => e -> BB.Builder
    go x
      | Just (s :: String) <- cast x = str s
      | Just (f :: FastString) <- cast x = str (unpackFS f)
      | Just (b :: B.ByteString) <- cast x = BB.string7 "(bytes " <> str (map (toEnum . fromIntegral) (B.unpack b)) <> BB.char7 ')'
      | Just (c :: HsDocStringChunk) <- cast x = BB.string7 "(chunk " <> str (unpackHDSC c) <> BB.char7 ')'
      | Just (r :: RdrName) <- cast x = rdr r
      | Just (m :: ModuleName) <- cast x = BB.string7 "(mod " <> str (moduleNameString m) <> BB.char7 ')'
      | Just (n :: OccName) <- cast x = occ "occ" n
      | Just (n :: Name) <- cast x = BB.string7 "(name " <> str (getOccString n) <> BB.char7 ')'
      | Just (st :: SourceText) <- cast x = case st of
          SourceText t -> BB.string7 "(src " <> str (unpackFS t) <> BB.char7 ')'
          NoSourceText -> BB.string7 "nil"
      | Just (sp :: SrcSpan) <- cast x = spanB modFile sp
      | Just (sp :: RealSrcSpan) <- cast x = spanB modFile (RealSrcSpan sp mempty')
      | Just (b :: Bool) <- cast x = if b then BB.string7 "true" else BB.string7 "false"
      | Just (i :: Int) <- cast x = BB.intDec i
      | Just (i :: Integer) <- cast x = BB.integerDec i
      | Just (i :: Word) <- cast x = BB.wordDec i
      | Just (c :: Char) <- cast x = BB.string7 "(char " <> BB.intDec (ord c) <> BB.char7 ')'
      | Just (r :: Rational) <- cast x = BB.string7 "(:% " <> BB.integerDec (numerator r) <> BB.char7 ' ' <> BB.integerDec (denominator r) <> BB.char7 ')'
      | Just (d :: Double) <- cast x = BB.string7 (show d)
      | tn == "GenLocated" = BB.string7 "(L " <> gmapQi 0 loc x <> BB.char7 ' ' <> gmapQi 1 go x <> BB.char7 ')'
      | tn == "NonEmpty" = BB.char7 '[' <> gmapQi 0 go x <> rest (gmapQi 1 listElems x) <> BB.char7 ']'
      | not full && isAnnType tn = BB.string7 "nil"
      | tn == "EpAnnComments" && not full = BB.string7 "nil"
      | otherwise = case dataTypeRep (dataTypeOf x) of
          AlgRep _ ->
            let c = showConstr (toConstr x)
            in if c == "(:)" then BB.char7 '[' <> mconcat (sepBy (BB.char7 ' ') (listElems x)) <> BB.char7 ']'
               else if c == "[]" then BB.string7 "[]"
               else if "(," `isPrefixOf` c then BB.string7 "(tuple " <> mconcat (sepBy (BB.char7 ' ') (gmapQ go x)) <> BB.char7 ')'
               else let kids = gmapQ go x in
                 if null kids then BB.stringUtf8 (conName c)
                 else BB.char7 '(' <> BB.stringUtf8 (conName c) <> BB.char7 ' ' <> mconcat (sepBy (BB.char7 ' ') kids) <> BB.char7 ')'
          NoRep -> BB.string7 "(hsdump/opaque " <> str tn <> BB.char7 ')'
          _ -> str (show (toConstr x))
      where tn = tyName x
    rest [] = mempty
    rest xs = BB.char7 ' ' <> mconcat (sepBy (BB.char7 ' ') xs)
    listElems :: forall e. Data e => e -> [BB.Builder]
    listElems y
      | showConstr (toConstr y) == "(:)" = gmapQi 0 go y : gmapQi 1 listElems y
      | otherwise = []
    loc :: forall e. Data e => e -> BB.Builder
    loc l
      | Just (sp :: SrcSpan) <- cast l = spanB modFile sp
      | Just (el :: EpaLocation) <- cast l = case el of
          EpaSpan sp -> spanB modFile sp
          EpaDelta sp _ _ -> spanB modFile sp
      | tyName l == "EpAnn" = gmapQi 0 loc l
      | otherwise = BB.string7 "nil"
    conName c = case c of
      ":|" -> "nonempty"
      _ -> c
    mempty' = GHC.Data.Strict.Nothing

-- spans: [l1 c1 l2 c2], the file name first when it is not the module's
spanB :: FastString -> SrcSpan -> BB.Builder
spanB modFile (RealSrcSpan r _) =
  BB.char7 '[' <> (if srcSpanFile r /= modFile then str (unpackFS (srcSpanFile r)) <> BB.char7 ' ' else mempty)
    <> BB.intDec (srcSpanStartLine r) <> BB.char7 ' ' <> BB.intDec (srcSpanStartCol r) <> BB.char7 ' '
    <> BB.intDec (srcSpanEndLine r) <> BB.char7 ' ' <> BB.intDec (srcSpanEndCol r) <> BB.char7 ']'
spanB _ _ = BB.string7 "nil"

nsName :: NameSpace -> String
nsName ns
  | isTcClsNameSpace ns = "tc"
  | isTvNameSpace ns = "tv"
  | isDataConNameSpace ns = "data"
  | isFieldNameSpace ns = "fld"
  | otherwise = "var"

occ :: String -> OccName -> BB.Builder
occ tag n = BB.char7 '(' <> BB.string7 tag <> BB.char7 ' ' <> BB.string7 (nsName (occNameSpace n)) <> BB.char7 ' ' <> str (occNameString n) <> BB.char7 ')'

rdr :: RdrName -> BB.Builder
rdr (Unqual o) = BB.string7 "(unqual " <> BB.string7 (nsName (occNameSpace o)) <> BB.char7 ' ' <> str (occNameString o) <> BB.char7 ')'
rdr (Qual m o) = BB.string7 "(qual " <> BB.string7 (nsName (occNameSpace o)) <> BB.char7 ' ' <> str (moduleNameString m) <> BB.char7 ' ' <> str (occNameString o) <> BB.char7 ')'
rdr (Orig m o) = BB.string7 "(orig " <> BB.string7 (nsName (occNameSpace o)) <> BB.char7 ' ' <> str (unitString (moduleUnit m)) <> BB.char7 ' ' <> str (moduleNameString (moduleName m)) <> BB.char7 ' ' <> str (occNameString o) <> BB.char7 ')'
rdr (Exact n) = BB.string7 "(exact " <> BB.string7 (nsName (occNameSpace (getOccName n))) <> BB.char7 ' ' <> str (getOccString n) <> BB.char7 ')'

-- ---------------------------------------------------------------------------------------
-- The tree level (SPEC §9.2): showAstData after eraseEpLayout, spans and annotations blanked
-- in :lines (every SourceText is blanked by BlankSrcSpan), nothing blanked in :full

treeOf :: Bool -> Located (HsModule GhcPs) -> String
treeOf full m =
  showSDocUnsafe (if full then showAstData NoBlankSrcSpan NoBlankEpAnnotations m
                  else showAstData BlankSrcSpan BlankEpAnnotations (blankSourceText (eraseEpLayout m))) ++ "\n"

-- every SourceText made NoSourceText (SPEC §9.2, N3): showAstData's BlankSrcSpan blanks most,
-- not the ones its numericLit prints (HsIntPrim, HsWordPrim...)
blankSourceText :: forall a. Data a => a -> a
blankSourceText = everywhere' go
  where
    go :: forall b. Data b => b -> b
    go x = case eqT @b @SourceText of
      Just Refl -> NoSourceText
      Nothing -> x

-- check-ppr's eraseEpLayout, written anew (SPEC HS8): every EpLayout made EpNoLayout
eraseEpLayout :: forall a. Data a => a -> a
eraseEpLayout = everywhere' go
  where
    go :: forall b. Data b => b -> b
    go x = case eqT @b @EpLayout of
      Just Refl -> EpNoLayout
      Nothing -> x

everywhere' :: (forall b. Data b => b -> b) -> (forall a. Data a => a -> a)
everywhere' f = go
  where
    go :: forall a. Data a => a -> a
    go x = f (gmapT go x)

-- the start line of every located node, in the tree's order, with the node's constructor; a
-- span in another file (a CPP inclusion) with its file name
linesOf :: FastString -> Located (HsModule GhcPs) -> BB.Builder
linesOf modFile m = go m
  where
    go :: forall e. Data e => e -> BB.Builder
    go x
      | Just (_ :: String) <- cast x = mempty
      | Just (_ :: FastString) <- cast x = mempty
      | tyName x == "GenLocated" =
          let here = gmapQi 0 start x
              what = gmapQi 1 conOf x
          in (if null here then mempty else BB.stringUtf8 here <> BB.char7 ' ' <> BB.stringUtf8 what <> BB.char7 '\n') <> gmapQi 1 go x
      | isAnnType (tyName x) || tyName x == "EpAnnComments" = mempty
      | otherwise = case dataTypeRep (dataTypeOf x) of
          AlgRep _ -> mconcat (gmapQ go x)
          _ -> mempty
    conOf :: forall e. Data e => e -> String
    conOf y = case dataTypeRep (dataTypeOf y) of
      AlgRep _ -> showConstr (toConstr y)
      _ -> tyName y
    start :: forall e. Data e => e -> String
    start l
      | Just (sp :: SrcSpan) <- cast l = sp1 sp
      | Just (el :: EpaLocation) <- cast l = case el of EpaSpan sp -> sp1 sp; _ -> ""
      | tyName l == "EpAnn" = gmapQi 0 start l
      | otherwise = ""
    sp1 (RealSrcSpan r _) = (if srcSpanFile r /= modFile then unpackFS (srcSpanFile r) ++ ":" else "") ++ show (srcSpanStartLine r)
    sp1 _ = ""

-- ---------------------------------------------------------------------------------------
-- SHA-256 (FIPS 180-4), for the configuration's hashes: GHC ships no SHA-256

sha256Hex :: B.ByteString -> String
sha256Hex msg = concatMap hex8 (go h0 (pad msg))
  where
    hex8 w = let s = showHex' (fromIntegral w) in replicate (8 - length s) '0' ++ s
    h0 = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19] :: [Word32]
    pad m = let l = fromIntegral (B.length m) * 8 :: Word64
                z = (55 - B.length m) `mod` 64
            in B.concat [m, B.singleton 0x80, B.replicate z 0, B.pack [fromIntegral (l `shiftR` (8 * i)) | i <- [7, 6 .. 0]]]
    go h m | B.null m = h
           | otherwise = let (blk, r) = B.splitAt 64 m in go (step h blk) r
    step h blk =
      let w16 = [word32 (B.take 4 (B.drop (4 * i) blk)) | i <- [0 .. 15]]
          ws = schedule w16
          [a, b, c, d, e, f, g, hh] = h
          (a', b', c', d', e', f', g', h') = foldl round' (a, b, c, d, e, f, g, hh) (zip kTable ws)
      in zipWith (+) h [a', b', c', d', e', f', g', h']
    word32 bs = foldl (\acc x -> (acc `shiftL` 8) .|. fromIntegral x) 0 (B.unpack bs)
    schedule w16 = take 64 ws where
      ws = w16 ++ zipWith4' (\w2 w7 w15 w16' -> s1 w2 + w7 + s0 w15 + w16') (drop 14 ws) (drop 9 ws) (drop 1 ws) ws
      s0 x = rotateR x 7 `xor` rotateR x 18 `xor` shiftR x 3
      s1 x = rotateR x 17 `xor` rotateR x 19 `xor` shiftR x 10
    zipWith4' f (a : as) (b : bs) (c : cs) (d : ds) = f a b c d : zipWith4' f as bs cs ds
    zipWith4' _ _ _ _ _ = []
    round' (!a, !b, !c, !d, !e, !f, !g, !h) (k, w) =
      let t1 = h + (rotateR e 6 `xor` rotateR e 11 `xor` rotateR e 25) + ((e .&. f) `xor` (complement e .&. g)) + k + w
          t2 = (rotateR a 2 `xor` rotateR a 13 `xor` rotateR a 22) + ((a .&. b) `xor` (a .&. c) `xor` (b .&. c))
      in (t1 + t2, a, b, c, d + t1, e, f, g)
    kTable = [0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
              0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
              0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
              0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
              0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
              0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
              0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
              0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2] :: [Word32]
