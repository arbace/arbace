#!r6rs
(library (srfi :133)
  (export vector-unfold vector-unfold-right vector-copy vector-reverse-copy vector-append vector-concatenate 
vector-append-subvectors vector-empty? vector= vector-fold vector-fold-right vector-map vector-map! vector-for-each 
vector-count vector-cumulate vector-index vector-index-right vector-skip vector-skip-right vector-binary-search 
vector-any vector-every vector-partition vector-swap! vector-fill! vector-reverse! vector-copy! vector-reverse-copy! 
vector-unfold! vector-unfold-right! vector->list reverse-vector->list list->vector reverse-list->vector vector->string 
string->vector)
  (import (srfi :133 vectors)))
