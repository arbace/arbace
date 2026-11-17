main :: IO ()
main = print (nfib 32)
nfib :: Int -> Int
nfib n = if n < 2 then 1 else nfib (n-1) + nfib (n-2) + 1
