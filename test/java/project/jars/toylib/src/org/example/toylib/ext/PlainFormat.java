package org.example.toylib.ext;

/** An extension that resolves: kept. */
public class PlainFormat implements Format {
    @Override
    public String format(String s) {
        return s;
    }
}
