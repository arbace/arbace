;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;; Arbace (hand change 17, doc/VENDOR-NOTES.md): this namespace needs the module java.desktop, which
;; Arbace's runtime images leave out unless asked for (bin/arbace-image, ARBACE_IMAGE_MODULES)
(when-not (.isPresent (.findModule (ModuleLayer/boot) "java.desktop"))
  (throw (UnsupportedOperationException.
           "arbace.java.browse-ui needs the module java.desktop, which this Java runtime does not have (an Arbace image holds java.base and jdk.unsupported; bin/arbace-image adds more with ARBACE_IMAGE_MODULES=java.desktop)")))

(ns
    ^{:author "Christophe Grand",
      :doc "Helper namespace for arbace.java.browse.
            Prevents console apps from becoming GUI unnecessarily."}
  arbace.java.browse-ui)

(defn- open-url-in-swing
  [url]
  (let [htmlpane (javax.swing.JEditorPane. url)]
    (.setEditable htmlpane false)
    (.addHyperlinkListener htmlpane
      (proxy [javax.swing.event.HyperlinkListener] []
        (hyperlinkUpdate [^javax.swing.event.HyperlinkEvent e]
          (when (= (.getEventType e) (. javax.swing.event.HyperlinkEvent$EventType ACTIVATED))
            (if (instance? javax.swing.text.html.HTMLFrameHyperlinkEvent e)
              (-> htmlpane .getDocument (.processHTMLFrameHyperlinkEvent e))
              (.setPage htmlpane (.getURL e)))))))
    (doto (javax.swing.JFrame.)
      (.setContentPane (javax.swing.JScrollPane. htmlpane))
      (.setBounds 32 32 700 900)
      (.setVisible true))))

