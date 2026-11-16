package org.example.toyext;

/** A binary-only library's interface (as OSGi's Bundle). */
public interface ToySource {
    String read() throws ToyException;
}
