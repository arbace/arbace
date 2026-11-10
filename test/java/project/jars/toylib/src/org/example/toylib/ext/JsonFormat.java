package org.example.toylib.ext;

import com.fasterxml.jackson.core.JsonFactory;

/** An extension naming a type of an optional dependency no jar here has: left out. */
public class JsonFormat implements Format {
    private final JsonFactory factory = new JsonFactory();

    @Override
    public String format(String s) {
        return factory.toString() + s;
    }
}
