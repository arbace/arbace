{-# LANGUAGE RecordWildCards, NamedFieldPuns, DuplicateRecordFields #-}
module R26Records2 where

data Config = Config { host :: String, port :: Int, debug :: Bool }
data Server = Server { name :: String, conf :: Config }

defaults :: Config
defaults = Config { host = "localhost", port = 80, debug = False }

mk :: String -> Int -> Config
mk host port = Config { debug = True, .. }

upd :: Config -> Config
upd c = c { port = 8080, debug = not (debug c) }

showC :: Config -> String
showC Config {..} = host ++ ":" ++ show port

pun :: Server -> String
pun Server { name, conf = Config { host } } = name ++ host

nested :: Server -> Int
nested s = port (conf s)
