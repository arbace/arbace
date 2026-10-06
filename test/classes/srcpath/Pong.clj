(in-ns 'classes.srcpath)

(defclass ^:public Pong
  (field ^:public ^:static ^:final ^int BASE 41)
  (method ^:static pong ^int [^int n]
    (if (<= n 0) 0 (unchecked-add-int 10 (Ping/ping (unchecked-dec-int n))))))
