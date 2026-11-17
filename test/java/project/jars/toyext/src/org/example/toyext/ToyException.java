package org.example.toyext;

/** A binary-only library's checked exception (as jdom2's JDOMException). */
public class ToyException extends Exception {
    public ToyException(String message) {
        super(message);
    }
}
