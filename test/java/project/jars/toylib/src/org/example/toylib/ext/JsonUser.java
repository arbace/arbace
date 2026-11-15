package org.example.toylib.ext;

/** Names only the left-out JsonFormat: left out after it, by the cascade. */
public class JsonUser {
    public static Format json() {
        return new JsonFormat();
    }
}
