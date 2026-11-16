module T16MR where

spri s = 0
opri c = 1

f c s = if spri s > opri c then f c s else 'b'

g c s = if spri s > opri c then 'a' else 'b'
