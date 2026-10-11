;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; /* rich Dec 17, 2007 */
;;
;; Converted from clojure/lang/XMLHandler.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(org.xml.sax Attributes ContentHandler Locator SAXException)
        '(org.xml.sax.helpers DefaultHandler))

(defclass ^:public XMLHandler
  :extends DefaultHandler

  (field ^ContentHandler h)

  (constructor ^:public [this ^ContentHandler h] (set! (.-h this) h))

  (method ^:public setDocumentLocator ^void [this ^Locator locator]
    (.setDocumentLocator h locator))

  (method ^:public startDocument :throws [SAXException] ^void [this]
    (.startDocument h))

  (method ^:public endDocument :throws [SAXException] ^void [this]
    (.endDocument h))

  (method ^:public startPrefixMapping :throws [SAXException] ^void [this ^String prefix ^String uri]
    (.startPrefixMapping h prefix uri))

  (method ^:public endPrefixMapping :throws [SAXException] ^void [this ^String prefix]
    (.endPrefixMapping h prefix))

  (method ^:public startElement :throws [SAXException] ^void [this ^String uri ^String localName
                                                              ^String qName ^Attributes atts]
    (.startElement h uri localName qName atts))

  (method ^:public endElement :throws [SAXException] ^void [this ^String uri ^String localName
                                                            ^String qName]
    (.endElement h uri localName qName))

  (method ^:public characters :throws [SAXException] ^void [this ^char/1 ch ^int start ^int length]
    (.characters h ch start length))

  (method ^:public ignorableWhitespace :throws [SAXException] ^void [this ^char/1 ch ^int start
                                                                     ^int length]
    (.ignorableWhitespace h ch start length))

  (method ^:public processingInstruction :throws [SAXException] ^void [this ^String target
                                                                       ^String data]
    (.processingInstruction h target data))

  (method ^:public skippedEntity :throws [SAXException] ^void [this ^String name]
    (.skippedEntity h name)))
