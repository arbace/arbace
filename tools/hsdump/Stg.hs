{-# LANGUAGE ScopedTypeVariables, LambdaCase #-}
-- hsdump's STG and Core modes (doc/haskell/STG-SPEC.md, doc/haskell/HASKELL-EVAL-SURVEY.md §5, HES4 (a)):
-- the modules compiled as `ghc --make` compiles them, with a hook between STG and Cmm
-- (GHC.Driver.Hooks' stgToCmmHook) that writes each module's final STG (the CgStgTopBindings the
-- code generator receives, after unarisation and tag inference, with their free variables) and its
-- data constructors as STG forms, then lets GHC's own code generator go on; and a Core-to-Core pass
-- at the end of the simplifier's pipeline (a static plugin) that writes the module's simplified
-- Core as Core forms. Arbace's own code, written anew; GHC.Stg.Syntax's pretty printer
-- (pprGenStgTopBinding) and GHC.StgToJS are its models.
module Stg (stgMain, Mode(..)) where

import GHC
import GHC.Driver.Session
import GHC.Driver.Env (HscEnv(..))
import GHC.Driver.Hooks (Hooks(..))
import GHC.Driver.Plugins (Plugin(..), defaultPlugin, StaticPlugin(..), PluginWithArgs(..), Plugins(..), purePlugin)
import GHC.Core.Opt.Pipeline.Types (CoreToDo(..))
import GHC.Unit.Module.ModGuts (ModGuts(..))
import GHC.Stg.Syntax
import qualified GHC.StgToCmm as StgToCmm
import GHC.StgToCmm.Config (StgToCmmConfig(..))
import GHC.Types.Id (Id, idName, idType)
import GHC.Types.Name (Name, isExternalName, nameModule, getOccString, nameUnique)
import GHC.Types.Unique (getKey)
import GHC.Types.Var.Set (dVarSetElems)
import GHC.Types.Literal
import GHC.Types.ForeignCall
import GHC.Types.Tickish (GenTickish(..), StgTickish)
import GHC.Types.CostCentre (costCentreUserName)
import GHC.Types.Basic (Levity(..), FunctionOrData(..), TypeOrConstraint(..))
import GHC.Types.RepType (typePrimRep, runtimeRepPrimRep_maybe)
import GHC.Core (Expr(..), Bind(..), Alt(..), AltCon(..), CoreBind, CoreExpr)
import GHC.Core.Coercion (coercionRKind)
import GHC.Types.Var (Var, varName, varType)
import GHC.Core.DataCon (DataCon, dataConName, dataConTag, dataConRepArgTys, dataConRepStrictness, StrictnessMark(..), dataConWorkId)
import GHC.Core.TyCon (TyCon, tyConName, tyConDataCons, isAlgTyCon, PrimRep(..), PrimElemRep(..))
import GHC.Core.TyCo.Rep (Scaled(..))
import GHC.Core.Type (tyConAppTyCon_maybe)
import GHC.Builtin.PrimOps (PrimOp(..), primOpOcc, PrimCall(..))
import GHC.Types.Name.Occurrence (occNameString)
import GHC.Unit.Types (moduleUnit, unitString)
import GHC.Unit.Module.ModSummary (ModSummary(..))
import GHC.Unit.Module.Graph (mgModSummaries, mgModSummaries', ModuleGraphNode(..), ModuleNodeInfo(..), mkNodeKey, mgNodeDependencies)
import GHC.Unit.Module.ModSummary (msHsFilePath)
import GHC.Driver.Pipeline (compileFile)
import GHC.Driver.Phases (StopPhase(..))
import GHC.Unit.Module.Location (ModLocation(..))
import GHC.Utils.Outputable (Outputable, ppr, showSDocOneLine, defaultSDocContext)
import GHC.Data.FastString (unpackFS)
import GHC.Types.SrcLoc

import Control.Exception (SomeException, try, evaluate, bracket_)
import Control.Concurrent (forkIO)
import Control.Concurrent.MVar
import Control.Concurrent.QSem
import System.Environment (getExecutablePath)
import System.Process (readProcessWithExitCode)
import System.IO.Unsafe (unsafePerformIO)
import Control.Monad (forM, forM_, when, unless)
import Control.Monad.IO.Class (liftIO)
import qualified Data.ByteString as B
import qualified Data.ByteString.Builder as BB
import Data.Bits (shiftR, (.&.))
import Data.Char (ord)
import Data.IORef
import Data.List (intersperse, isPrefixOf)
import qualified Data.Map.Strict as Map
import qualified Data.IntMap.Strict as IM
import Data.Maybe (fromMaybe)
import Data.Word (Word8)
import GHC.Float (castDoubleToWord64, castFloatToWord32)
import System.Directory (createDirectoryIfMissing, renameFile)
import System.Exit (exitWith, ExitCode(..))
import System.FilePath ((</>))
import System.IO

-- stgMain VERB OUT FLAGS FILES HELPER-VERSION GHC-UNIT HEADER: verb "stg" (STG, and Core with
-- --core in FLAGS' place: the caller passes wantCore), "core" (Core only).
-- Modes: Make, the files compiled as ghc --make compiles them; OneShot (--one-shot), the modules of the files'
-- graph compiled each by a process of its own (this program again, mode Single, CHILD-ARGS FILE its arguments) in
-- dependency order, -jN of them at once, as hadrian runs `ghc -c` per module (HES6): a module that imports one through
-- an hs-boot file then sees the boot interface, and every interface is read afresh, as the installed libraries'
-- compilations did, where --make rehydrates a loop's modules with the real interfaces and keeps every interface it
-- read; Single (--single), one file compiled as ghc -c compiles it.
data Mode = Make | OneShotAll (FilePath -> [String]) | Single

stgMain :: Bool -> Bool -> Mode -> FilePath -> FilePath -> [String] -> [FilePath] -> [(String, String)] -> IO ()
stgMain wantStg wantCore mode libdir out ghcFlags files header = do
  createDirectoryIfMissing True out
  failed <- runGhc (Just libdir) $ do
    dflags0 <- getSessionDynFlags
    logger <- getLogger
    (dflags1, leftover, _) <- parseDynamicFlagsCmdLine logger dflags0 (map noLoc ghcFlags)
    unless (null leftover) $ liftIO $ do
      hPutStrLn stderr ("hsdump: unknown GHC flags: " ++ unwords (map unLoc leftover))
      exitWith (ExitFailure 2)
    _ <- setSessionDynFlags dflags1 { ghcLink = NoLink, ghcMode = case mode of Single -> OneShot; _ -> ghcMode dflags1 }
    srcs <- liftIO (newIORef Map.empty)
    env <- getSession
    let hook cfg ipm tycons ccs binds = do
          let m = stgToCmmThisModule cfg
          src <- liftIO (srcOf srcs m)
          liftIO (stgModule header m src tycons binds >>=
                    writeAtomically (out </> moduleNameString (moduleName m) ++ ".stg.ae"))
          fst <$> StgToCmm.codeGen (hsc_logger env) (hsc_tmpfs env) cfg ipm tycons ccs binds
        corePass guts = do
          let m = mg_module guts
          src <- liftIO (srcOf srcs m)
          liftIO (coreModuleB header m src (mg_binds guts) >>=
                    writeAtomically (out </> moduleNameString (moduleName m) ++ ".core.ae"))
          return guts
        plugin = defaultPlugin
          { installCoreToDos = \_ todos -> return (todos ++ [CoreDoPluginPass "hsdump-core" corePass])
          , pluginRecompile = purePlugin }
        hooks0 = hsc_hooks env
        plugins0 = hsc_plugins env
    setSession env
      { hsc_hooks = if wantStg then hooks0 { stgToCmmHook = Just hook } else hooks0
      , hsc_plugins = if wantCore
          then plugins0 { staticPlugins = staticPlugins plugins0 ++ [StaticPlugin (PluginWithArgs plugin []) False] }
          else plugins0 }
    case mode of
      Single -> do
        -- one module, its source the file given (the hook's module is the file's)
        env1 <- getSession
        rs <- liftIO $ forM files $ \f -> do
          writeIORef singleSrc f
          r <- try (compileFile env1 NoStop (f, Nothing)) :: IO (Either SomeException (Maybe FilePath))
          case r of
            Left e -> do hPutStrLn stderr ("hsdump: " ++ f ++ ": " ++ show e); return False
            Right _ -> return True
        return (not (and rs))
      _ -> do
        targets <- mapM (\f -> guessTarget f Nothing Nothing) files
        setTargets targets
        mg <- depanal [] False
        liftIO $ writeIORef srcs (Map.fromList
          [ (ms_mod s, fromMaybe "" (ml_hs_file (ms_location s))) | s <- mgModSummaries mg ])
        case mode of
          OneShotAll childArgs -> liftIO (scheduler (parallelism ghcFlags) childArgs mg)
          _ -> do
            ok <- load LoadAllTargets
            return (not (succeeded ok))
  when failed $ exitWith (ExitFailure 1)

-- the source file of a module: the module graph's, else (mode Single) the file being compiled
singleSrc :: IORef FilePath
singleSrc = unsafePerformIO (newIORef "")
{-# NOINLINE singleSrc #-}

srcOf :: IORef (Map.Map Module FilePath) -> Module -> IO FilePath
srcOf srcs m = do
  mp <- readIORef srcs
  case Map.lookup m mp of
    Just f -> return f
    Nothing -> readIORef singleSrc

-- -jN of the flags (1 without)
parallelism :: [String] -> Int
parallelism fs = case [ drop 2 f | f <- fs, "-j" `isPrefixOf` f ] of
  (n : _) | not (null n), all (`elem` "0123456789") n -> max 1 (read n)
  _ -> 1

-- every module of the graph compiled by a process of its own once its dependencies are, n at a time; True when one fails
scheduler :: Int -> (FilePath -> [String]) -> ModuleGraph -> IO Bool
scheduler n childArgs mg = do
  self <- getExecutablePath
  sem <- newQSem n
  let nodes = [ (mkNodeKey node, ms, mgNodeDependencies False node)
              | node@(ModuleNode _ (ModuleNodeCompile ms)) <- mgModSummaries' mg ]
  vars <- Map.fromList <$> mapM (\(k, _, _) -> (,) k <$> newEmptyMVar) nodes
  forM_ nodes $ \(k, ms, deps) -> forkIO $ do
    oks <- mapM readMVar [ v | d <- deps, Just v <- [Map.lookup d vars] ]
    ok <- if and oks
      then bracket_ (waitQSem sem) (signalQSem sem) $ do
        let f = msHsFilePath ms
        (code, o, e) <- readProcessWithExitCode self (childArgs f) ""
        unless (null o && null e) $ withMVar outLock (\_ -> hPutStr stderr (o ++ e))
        return (code == ExitSuccess)
      else return False
    putMVar (vars Map.! k) ok
  rs <- mapM readMVar (Map.elems vars)
  return (not (and rs))

outLock :: MVar ()
outLock = unsafePerformIO (newMVar ())
{-# NOINLINE outLock #-}

-- the file written through a temporary name, so that a dump is whole or absent
writeAtomically :: FilePath -> BB.Builder -> IO ()
writeAtomically f b = do
  let t = f ++ ".part"
  withFile t WriteMode $ \h -> hSetBinaryMode h True >> BB.hPutBuilder h b
  renameFile t f

-- ---------------------------------------------------------------------------------------
-- Names (STG-SPEC §3): an external name "unit:Module/occ"; an internal one "occ_N", N numbered
-- per module in the order of the dump (top-level binders first), so that two runs agree.

type Names = IORef (IM.IntMap Int, Int)

newNames :: IO Names
newNames = newIORef (IM.empty, 0)

nameB :: Names -> Name -> IO BB.Builder
nameB ns n
  | isExternalName n = return (strB (extName n))
  | otherwise = do
      let k = fromIntegral (getKey (nameUnique n)) :: Int
      (m, c) <- readIORef ns
      i <- case IM.lookup k m of
        Just i -> return i
        Nothing -> do writeIORef ns (IM.insert k c m, c + 1); return c
      return (strB (getOccString n ++ "_" ++ show i))

extName :: Name -> String
extName n = let m = nameModule n in
  unitString (moduleUnit m) ++ ":" ++ moduleNameString (moduleName m) ++ "/" ++ getOccString n

idB :: Names -> Id -> IO BB.Builder
idB ns = nameB ns . idName

-- ---------------------------------------------------------------------------------------
-- Text

strB :: String -> BB.Builder
strB s = BB.char7 '"' <> foldMap e s <> BB.char7 '"'
  where
    e '"' = BB.string7 "\\\""
    e '\\' = BB.string7 "\\\\"
    e '\n' = BB.string7 "\\n"
    e '\t' = BB.string7 "\\t"
    e '\r' = BB.string7 "\\r"
    e c | n < 32 || n == 127 || (n >= 0xD800 && n <= 0xDFFF) || (n >= 0x80 && n < 0xA0) = u4 n
        | otherwise = BB.charUtf8 c
      where n = ord c

-- bytes as a string of one character per byte (U+0000 to U+00FF; STG-SPEC §5)
bytesB :: B.ByteString -> BB.Builder
bytesB bs = BB.char7 '"' <> foldMap e (B.unpack bs) <> BB.char7 '"'
  where
    e :: Word8 -> BB.Builder
    e 34 = BB.string7 "\\\""
    e 92 = BB.string7 "\\\\"
    e 10 = BB.string7 "\\n"
    e w | w < 32 || w >= 127 = u4 (fromIntegral w)
        | otherwise = BB.word8 w

u4 :: Int -> BB.Builder
u4 n = BB.string7 "\\u" <> BB.string7 (pad (hex n)) where pad h = replicate (4 - length h) '0' ++ h

hex :: Integral a => a -> String
hex 0 = "0"
hex n0 = reverse (go n0) where
  go 0 = ""
  go k = "0123456789abcdef" !! fromIntegral (k `mod` 16) : go (k `div` 16)

sp :: BB.Builder
sp = BB.char7 ' '

form :: String -> [BB.Builder] -> BB.Builder
form h xs = BB.char7 '(' <> BB.string7 h <> foldMap (sp <>) xs <> BB.char7 ')'

vec :: [BB.Builder] -> BB.Builder
vec xs = BB.char7 '[' <> mconcat (intersperse sp xs) <> BB.char7 ']'

kw :: String -> BB.Builder
kw s = BB.char7 ':' <> BB.string7 s

pprS :: Outputable a => a -> String
pprS = showSDocOneLine defaultSDocContext . ppr

-- ---------------------------------------------------------------------------------------
-- Representations (STG-SPEC §4)

repB :: PrimRep -> BB.Builder
repB = \case
  BoxedRep (Just Lifted) -> kw "L"
  BoxedRep (Just Unlifted) -> kw "U"
  BoxedRep Nothing -> kw "B"
  IntRep -> kw "int"; Int8Rep -> kw "int8"; Int16Rep -> kw "int16"; Int32Rep -> kw "int32"; Int64Rep -> kw "int64"
  WordRep -> kw "word"; Word8Rep -> kw "word8"; Word16Rep -> kw "word16"; Word32Rep -> kw "word32"; Word64Rep -> kw "word64"
  AddrRep -> kw "addr"; FloatRep -> kw "float"; DoubleRep -> kw "double"
  VecRep n e -> vec [kw "vec", BB.intDec n, elemB e]
  where
    elemB = \case
      Int8ElemRep -> kw "int8"; Int16ElemRep -> kw "int16"; Int32ElemRep -> kw "int32"; Int64ElemRep -> kw "int64"
      Word8ElemRep -> kw "word8"; Word16ElemRep -> kw "word16"; Word32ElemRep -> kw "word32"; Word64ElemRep -> kw "word64"
      FloatElemRep -> kw "float"; DoubleElemRep -> kw "double"

-- a list of representations: one as itself, none :v, several a vector
repsB :: [PrimRep] -> BB.Builder
repsB [] = kw "v"
repsB [r] = repB r
repsB rs = vec (map repB rs)

-- a type's representations; a representation-polymorphic type (a binder of kind TYPE r, r a variable: GHC's
-- typePrimRep panics) is :rp (STG-SPEC §4)
typeRepsB :: Type -> IO BB.Builder
typeRepsB t = do
  r <- try (evaluate (let rs = typePrimRep t in length (show rs) `seq` rs)) :: IO (Either SomeException [PrimRep])
  return (either (const (kw "rp")) repsB r)

binderB :: Names -> Id -> IO BB.Builder
binderB ns x = do
  n <- idB ns x
  r <- typeRepsB (idType x)
  return (vec [n, r])

-- ---------------------------------------------------------------------------------------
-- Literals (STG-SPEC §5)

litB :: Literal -> BB.Builder
litB = \case
  LitChar c -> form "char" [BB.intDec (ord c)]
  LitNumber t n -> form (numTy t) [BB.integerDec n]
  LitString bs -> form "str" [bytesB bs]
  LitNullAddr -> form "nulladdr" []
  LitRubbish tc rr -> form "rubbish" [kw (case tc of TypeLike -> "type"; ConstraintLike -> "constraint"),
                                      maybe (strB (pprS rr)) repsB (runtimeRepPrimRep_maybe rr)]
  LitFloat r -> form "float" [BB.string7 ("0x" ++ hex (castFloatToWord32 (fromRational r)))]
  LitDouble r -> form "double" [BB.string7 ("0x" ++ hex (castDoubleToWord64 (fromRational r)))]
  LitLabel fs fod -> form "label" [strB (unpackFS fs), kw (case fod of IsFunction -> "fun"; IsData -> "data")]
  where
    numTy = \case
      LitNumBigNat -> "bignat"; LitNumInt -> "int"; LitNumInt8 -> "int8"; LitNumInt16 -> "int16"
      LitNumInt32 -> "int32"; LitNumInt64 -> "int64"; LitNumWord -> "word"; LitNumWord8 -> "word8"
      LitNumWord16 -> "word16"; LitNumWord32 -> "word32"; LitNumWord64 -> "word64"

argB :: Names -> StgArg -> IO BB.Builder
argB ns (StgVarArg x) = idB ns x
argB _ (StgLitArg l) = return (litB l)

argsB :: Names -> [StgArg] -> IO BB.Builder
argsB ns as = vec <$> mapM (argB ns) as

-- ---------------------------------------------------------------------------------------
-- The module (STG-SPEC §2, §6, §7)

headerB :: [(String, String)] -> String -> Module -> FilePath -> BB.Builder
headerB header h m src =
  form h [BB.char7 '{' <> mconcat (intersperse sp
    ([kw "unit" <> sp <> strB (unitString (moduleUnit m)), kw "module" <> sp <> strB (moduleNameString (moduleName m)),
      kw "file" <> sp <> strB src] ++ [kw k <> sp <> v | (k, v) <- map (fmap BB.string7) header])) <> BB.char7 '}']

stgModule :: [(String, String)] -> Module -> FilePath -> [TyCon] -> [CgStgTopBinding] -> IO BB.Builder
stgModule header m src tycons binds = do
  ns <- newNames
  -- top-level binders first, so that a top-level internal name has the same number wherever it occurs
  forM_ binds $ \case
    StgTopLifted (StgNonRec b _) -> () <$ idB ns b
    StgTopLifted (StgRec bs) -> forM_ bs (\(b, _) -> idB ns b)
    StgTopStringLit b _ -> () <$ idB ns b
  ds <- mapM (dataB ns) (filter isAlgTyCon tycons)
  bs <- mapM (topB ns) binds
  return (headerB header "stg/module" m src <> BB.char7 '\n' <> foldMap (<> BB.char7 '\n') (ds ++ bs))

dataB :: Names -> TyCon -> IO BB.Builder
dataB ns tc = do
  t <- nameB ns (tyConName tc)
  cs <- mapM conB (tyConDataCons tc)
  return (form "stg/data" [t, vec cs])
  where
    conB dc = do
      n <- nameB ns (dataConName dc)
      w <- idB ns (dataConWorkId dc)
      fields <- mapM (\(Scaled _ t) -> typeRepsB t) (dataConRepArgTys dc)
      let marks = [ kw (case s of MarkedStrict -> "strict"; NotMarkedStrict -> "lazy") | s <- dataConRepStrictness dc ]
          same = isExternalName (dataConName dc) && isExternalName (idName (dataConWorkId dc))
                 && extName (dataConName dc) == extName (idName (dataConWorkId dc))
      return (form "dc" ([n, BB.intDec (dataConTag dc), vec fields, vec marks] ++ [w | not same]))

topB :: Names -> CgStgTopBinding -> IO BB.Builder
topB ns = \case
  StgTopLifted b -> bindB ns b
  StgTopStringLit b bs -> do x <- binderB ns b; return (form "string" [x, bytesB bs])

bindB :: Names -> CgStgBinding -> IO BB.Builder
bindB ns = \case
  StgNonRec b r -> one b r
  StgRec bs -> form "rec" <$> mapM (uncurry one) bs
  where one b r = do x <- binderB ns b; y <- rhsB ns r; return (form "bind" [x, y])

rhsB :: Names -> CgStgRhs -> IO BB.Builder
rhsB ns = \case
  StgRhsClosure fvs _ccs upd args body _ty -> do
    f <- mapM (idB ns) (dVarSetElems fvs)
    as <- mapM (binderB ns) args
    e <- exprB ns body
    return (form "closure" [kw (updS upd), vec f, vec as, e])
  StgRhsCon _ccs dc cn ticks args _ty -> do
    d <- nameB ns (dataConName dc)
    as <- argsB ns args
    ts <- mapM (tickB ns) ticks
    return (form "rcon" ([d, as] ++ conNum cn ++ [vec ts | not (null ts)]))
  where
    updS = \case ReEntrant -> "r"; Updatable -> "u"; SingleEntry -> "s"; JumpedTo -> "j"

conNum :: ConstructorNumber -> [BB.Builder]
conNum NoNumber = []
conNum (Numbered n) = [BB.char7 '{' <> kw "number" <> sp <> BB.intDec n <> BB.char7 '}']

exprB :: Names -> CgStgExpr -> IO BB.Builder
exprB ns = \case
  StgApp f as -> do g <- idB ns f; a <- argsB ns as; return (form "app" [g, a])
  StgLit l -> return (litB l)
  StgConApp dc cn as reps0 -> do
    d <- nameB ns (dataConName dc)
    a <- argsB ns as
    -- the representations are a panic where tag inference rebuilt the application
    -- (GHC.Stg.EnforceEpt.Rewrite's mkSeqs: "shouldn't use the type arg"): written only when they evaluate
    r <- try (evaluate (length (show reps0) `seq` reps0)) :: IO (Either SomeException [[PrimRep]])
    let reps = either (const []) id r
    return (form "con" ([d, a] ++ [vec (map repsB reps) | not (null reps)] ++ conNum cn))
  StgOpApp op as ty -> do
    a <- argsB ns as
    case op of
      StgPrimOp p -> do
        extra <- case p of
          TagToEnumOp | Just tc <- tyConAppTyCon_maybe ty -> (\t -> [BB.char7 '{' <> kw "tycon" <> sp <> t <> BB.char7 '}']) <$> nameB ns (tyConName tc)
          _ -> return []
        return (form "primop" ([strB (occNameString (primOpOcc p)), a] ++ extra))
      StgPrimCallOp (PrimCall lbl u) -> do
        r <- typeRepsB ty
        return (form "primcall" [strB (unpackFS lbl), strB (unitString u), a, r])
      StgFCallOp (CCall (CCallSpec target conv safety)) _ -> do
        r <- typeRepsB ty
        return (form "ffi" [targetB target, kw (convS conv), kw (safetyS safety), a, r])
  StgCase e b alt alts -> do
    s <- exprB ns e
    x <- binderB ns b
    t <- altTypeB alt
    as <- mapM altB alts
    return (form "case" [s, x, t, vec as])
  StgLet _ b e -> do x <- bindB ns b; y <- exprB ns e; return (form "let" [x, y])
  StgLetNoEscape _ b e -> do x <- bindB ns b; y <- exprB ns e; return (form "letne" [x, y])
  StgTick t e -> do x <- tickB ns t; y <- exprB ns e; return (form "tick" [x, y])
  where
    altTypeB = \case
      PolyAlt -> return (form "poly" [])
      MultiValAlt n -> return (form "multi" [BB.intDec n])
      AlgAlt tc -> (\t -> form "alg" [t]) <$> nameB ns (tyConName tc)
      PrimAlt r -> return (form "prim" [repB r])
    altB (GenStgAlt con bs rhs) = do
      c <- case con of
        DEFAULT -> return (kw "default")
        DataAlt dc -> nameB ns (dataConName dc)
        LitAlt l -> return (litB l)
      xs <- mapM (binderB ns) bs
      e <- exprB ns rhs
      return (form "alt" [c, vec xs, e])
    targetB = \case
      StaticTarget _ lbl mu isFun -> form "static" ([strB (unpackFS lbl)] ++
        [BB.char7 '{' <> mconcat (intersperse sp (
          [kw "unit" <> sp <> strB (unitString u) | Just u <- [mu]] ++
          [kw "fun" <> sp <> BB.string7 "false" | not isFun])) <> BB.char7 '}' | maybe False (const True) mu || not isFun])
      DynamicTarget -> form "dynamic" []
    convS = \case CCallConv -> "ccall"; CApiConv -> "capi"; StdCallConv -> "stdcall"; PrimCallConv -> "prim"; JavaScriptCallConv -> "javascript"
    safetyS = \case PlaySafe -> "safe"; PlayInterruptible -> "interruptible"; PlayRisky -> "unsafe"

tickB :: Names -> StgTickish -> IO BB.Builder
tickB ns = \case
  ProfNote cc cnt scope -> return (form "scc" [strB (costCentreUserName cc), bool cnt, bool scope])
  HpcTick m n -> return (form "hpc" [strB (moduleNameString (moduleName m)), BB.intDec n])
  Breakpoint _ bid fvs -> do
    xs <- mapM (idB ns) fvs
    return (form "break" [strB (pprS bid), vec xs])
  SourceNote s n -> return (form "src" [strB (unpackFS (srcSpanFile s)), BB.intDec (srcSpanStartLine s), BB.intDec (srcSpanStartCol s),
                                        BB.intDec (srcSpanEndLine s), BB.intDec (srcSpanEndCol s), strB (pprS n)])
  where
    bool b = BB.string7 (if b then "true" else "false")

-- ---------------------------------------------------------------------------------------
-- Core (STG-SPEC §9): the simplified Core, for the REPL's types and for debugging; types as GHC prints them

coreModuleB :: [(String, String)] -> Module -> FilePath -> [CoreBind] -> IO BB.Builder
coreModuleB header m src binds = do
  ns <- newNames
  forM_ binds $ \case
    NonRec b _ -> () <$ cbndr ns b
    Rec bs -> forM_ bs (\(b, _) -> cbndr ns b)
  bs <- mapM (cbind ns) binds
  return (headerB header "core/module" m src <> BB.char7 '\n' <> foldMap (<> BB.char7 '\n') bs)
  where
    cbndr ns b = nameB ns (varName b)

cbind :: Names -> CoreBind -> IO BB.Builder
cbind ns = \case
  NonRec b e -> one b e
  Rec bs -> form "rec" <$> mapM (uncurry one) bs
  where one b e = do x <- cvarB ns b; y <- cexpr ns e; return (form "bind" [x, y])

cvarB :: Names -> Var -> IO BB.Builder
cvarB ns v = do
  n <- nameB ns (varName v)
  return (vec [n, strB (pprS (varType v))])

cexpr :: Names -> CoreExpr -> IO BB.Builder
cexpr ns = \case
  Var v -> nameB ns (varName v)
  Lit l -> return (litB l)
  e@App{} -> do
    let (f, as) = collect e []
    g <- cexpr ns f
    xs <- mapM (cexpr ns) as
    return (form "app" [g, vec xs])
  e@Lam{} -> do
    let (bs, body) = lams e []
    xs <- mapM (cvarB ns) bs
    y <- cexpr ns body
    return (form "lam" [vec xs, y])
  Let b e -> do x <- cbind ns b; y <- cexpr ns e; return (form "let" [x, y])
  Case e b t alts -> do
    s <- cexpr ns e
    x <- cvarB ns b
    as <- mapM calt alts
    return (form "case" [s, x, strB (pprS t), vec as])
  Cast e co -> do x <- cexpr ns e; return (form "cast" [x, strB (pprS (coercionRKind co))])
  Tick t e -> do x <- cexpr ns e; return (form "tick" [strB (pprS t), x])
  Type t -> return (form "type" [strB (pprS t)])
  Coercion co -> return (form "coercion" [strB (pprS (coercionRKind co))])
  where
    collect (App f a) acc = collect f (a : acc)
    collect f acc = (f, acc)
    lams (Lam b e) acc = lams e (b : acc)
    lams e acc = (reverse acc, e)
    calt (Alt con bs rhs) = do
      c <- case con of
        DEFAULT -> return (kw "default")
        DataAlt dc -> nameB ns (dataConName dc)
        LitAlt l -> return (litB l)
      xs <- mapM (cvarB ns) bs
      e <- cexpr ns rhs
      return (form "alt" [c, vec xs, e])
