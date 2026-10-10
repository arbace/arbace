/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package com.sun.crypto.provider;

import java.security.NoSuchAlgorithmException;

/**
 * The SunJCE services of the Go build (doc/go/JRT-NOTES.md, "Security"), constructed directly for
 * sun.security.provider.JrtServices (HmacCore and its nested classes are package-private).
 */
public final class JrtServices {
    private JrtServices() {
    }

    /** A new instance of the SunJCE service class className, or null if it is not one. */
    public static Object newInstance(String className) throws NoSuchAlgorithmException {
        switch (className) {
            case "com.sun.crypto.provider.HmacMD5":
                return new HmacMD5();
            case "com.sun.crypto.provider.HmacSHA1":
                return new HmacSHA1();
            case "com.sun.crypto.provider.HmacCore$HmacSHA224":
                return new HmacCore.HmacSHA224();
            case "com.sun.crypto.provider.HmacCore$HmacSHA256":
                return new HmacCore.HmacSHA256();
            case "com.sun.crypto.provider.HmacCore$HmacSHA384":
                return new HmacCore.HmacSHA384();
            case "com.sun.crypto.provider.HmacCore$HmacSHA512":
                return new HmacCore.HmacSHA512();
            case "com.sun.crypto.provider.HmacCore$HmacSHA512_224":
                return new HmacCore.HmacSHA512_224();
            case "com.sun.crypto.provider.HmacCore$HmacSHA512_256":
                return new HmacCore.HmacSHA512_256();
            case "com.sun.crypto.provider.HmacCore$HmacSHA3_224":
                return new HmacCore.HmacSHA3_224();
            case "com.sun.crypto.provider.HmacCore$HmacSHA3_256":
                return new HmacCore.HmacSHA3_256();
            case "com.sun.crypto.provider.HmacCore$HmacSHA3_384":
                return new HmacCore.HmacSHA3_384();
            case "com.sun.crypto.provider.HmacCore$HmacSHA3_512":
                return new HmacCore.HmacSHA3_512();
            default:
                return null;
        }
    }
}
