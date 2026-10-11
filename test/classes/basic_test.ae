(ns classes.basic-test
  (:require [arbace.test :refer :all]
            [classes.helpers :refer :all]))

(deftest reduced-shape
  (same-shapes? 'classes.basic-test
    {"Reduced" "public final class Reduced implements clojure.lang.IDeref {
                  Object val;
                  public Reduced(Object val) { this.val = val; }
                  public Object deref() { return val; } }"}
    '[(^:public ^:final Reduced
        :implements [clojure.lang.IDeref]
        (field val)
        (constructor ^:public [this val] (set! (.-val this) val))
        (method ^:public deref [this] val))]))
