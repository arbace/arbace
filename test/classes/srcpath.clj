;; A package laid out as SPEC §9.1 says: the namespace loads one file per class. Ping and Pong
;; refer to each other; compiling Ping finds Pong through the source path (§9.2).
(ns classes.srcpath)

(load "srcpath/Ping")
(load "srcpath/Pong")
