(in-ns 'classes.srcpath)

(defclass ^:public Ping
  (field ^:public ^:static ^:final ^int LIMIT (unchecked-add-int Pong/BASE 1))
  (method ^:public ^:static ping ^int [^int n]
    (if (<= n 0) 0 (unchecked-add-int 1 (Pong/pong (unchecked-dec-int n))))))
