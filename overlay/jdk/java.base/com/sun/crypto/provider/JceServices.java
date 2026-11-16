/*
 * Copyright (c) the Arbace authors. Eclipse Public License 1.0 (LICENSE.md).
 */
package com.sun.crypto.provider;

import javax.crypto.KDFParameters;

/**
 * The SunJCE services of the Go build (doc/go/JRT-NOTES.md, "Security"; doc/classes/JAVA-DEFERRED-TRIAGE.md,
 * F1), constructed directly for sun.security.provider.JrtServices (most of the classes, and the
 * nested ones, are package-private): every service class jdk26u's SunJCE.putEntries names, in its
 * order, but the KeyStore JCEKS (JceKeyStore: java.security.KeyStore is not in the Go build yet,
 * F2), for which newInstance answers null, as for a class it does not know. Generated from
 * jdk26u's SunJCE.java (baf63fb) by the F1 scratch script gensvc.py/genjava.py.
 */
public final class JceServices {
    private JceServices() {
    }

    /** A new instance of the SunJCE service class className, or null if it is not one. */
    public static Object newInstance(String className) throws Exception {
        switch (className) {
            case "com.sun.crypto.provider.RSACipherAdaptor":
                return new RSACipherAdaptor();
            case "com.sun.crypto.provider.RSACipher":
                return new RSACipher();
            case "com.sun.crypto.provider.DESCipher":
                return new DESCipher();
            case "com.sun.crypto.provider.DESedeCipher":
                return new DESedeCipher();
            case "com.sun.crypto.provider.BlowfishCipher":
                return new BlowfishCipher();
            case "com.sun.crypto.provider.RC2Cipher":
                return new RC2Cipher();
            case "com.sun.crypto.provider.AESCipher$General":
                return new AESCipher.General();
            case "com.sun.crypto.provider.KeyWrapCipher$AES_KW_NoPadding":
                return new KeyWrapCipher.AES_KW_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES_KW_PKCS5Padding":
                return new KeyWrapCipher.AES_KW_PKCS5Padding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES_KWP_NoPadding":
                return new KeyWrapCipher.AES_KWP_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES128_ECB_NoPadding":
                return new AESCipher.AES128_ECB_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES128_CBC_NoPadding":
                return new AESCipher.AES128_CBC_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES128_OFB_NoPadding":
                return new AESCipher.AES128_OFB_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES128_CFB_NoPadding":
                return new AESCipher.AES128_CFB_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES128_KW_NoPadding":
                return new KeyWrapCipher.AES128_KW_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES128_KW_PKCS5Padding":
                return new KeyWrapCipher.AES128_KW_PKCS5Padding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES128_KWP_NoPadding":
                return new KeyWrapCipher.AES128_KWP_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES192_ECB_NoPadding":
                return new AESCipher.AES192_ECB_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES192_CBC_NoPadding":
                return new AESCipher.AES192_CBC_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES192_OFB_NoPadding":
                return new AESCipher.AES192_OFB_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES192_CFB_NoPadding":
                return new AESCipher.AES192_CFB_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES192_KW_NoPadding":
                return new KeyWrapCipher.AES192_KW_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES192_KW_PKCS5Padding":
                return new KeyWrapCipher.AES192_KW_PKCS5Padding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES192_KWP_NoPadding":
                return new KeyWrapCipher.AES192_KWP_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES256_ECB_NoPadding":
                return new AESCipher.AES256_ECB_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES256_CBC_NoPadding":
                return new AESCipher.AES256_CBC_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES256_OFB_NoPadding":
                return new AESCipher.AES256_OFB_NoPadding();
            case "com.sun.crypto.provider.AESCipher$AES256_CFB_NoPadding":
                return new AESCipher.AES256_CFB_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES256_KW_NoPadding":
                return new KeyWrapCipher.AES256_KW_NoPadding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES256_KW_PKCS5Padding":
                return new KeyWrapCipher.AES256_KW_PKCS5Padding();
            case "com.sun.crypto.provider.KeyWrapCipher$AES256_KWP_NoPadding":
                return new KeyWrapCipher.AES256_KWP_NoPadding();
            case "com.sun.crypto.provider.GaloisCounterMode$AESGCM":
                return new GaloisCounterMode.AESGCM();
            case "com.sun.crypto.provider.GaloisCounterMode$AES128":
                return new GaloisCounterMode.AES128();
            case "com.sun.crypto.provider.GaloisCounterMode$AES192":
                return new GaloisCounterMode.AES192();
            case "com.sun.crypto.provider.GaloisCounterMode$AES256":
                return new GaloisCounterMode.AES256();
            case "com.sun.crypto.provider.DESedeWrapCipher":
                return new DESedeWrapCipher();
            case "com.sun.crypto.provider.ARCFOURCipher":
                return new ARCFOURCipher();
            case "com.sun.crypto.provider.ChaCha20Cipher$ChaCha20Only":
                return new ChaCha20Cipher.ChaCha20Only();
            case "com.sun.crypto.provider.ChaCha20Cipher$ChaCha20Poly1305":
                return new ChaCha20Cipher.ChaCha20Poly1305();
            case "com.sun.crypto.provider.PBEWithMD5AndDESCipher":
                return new PBEWithMD5AndDESCipher();
            case "com.sun.crypto.provider.PBEWithMD5AndTripleDESCipher":
                return new PBEWithMD5AndTripleDESCipher();
            case "com.sun.crypto.provider.PKCS12PBECipherCore$PBEWithSHA1AndDESede":
                return new PKCS12PBECipherCore.PBEWithSHA1AndDESede();
            case "com.sun.crypto.provider.PKCS12PBECipherCore$PBEWithSHA1AndRC2_40":
                return new PKCS12PBECipherCore.PBEWithSHA1AndRC2_40();
            case "com.sun.crypto.provider.PKCS12PBECipherCore$PBEWithSHA1AndRC2_128":
                return new PKCS12PBECipherCore.PBEWithSHA1AndRC2_128();
            case "com.sun.crypto.provider.PKCS12PBECipherCore$PBEWithSHA1AndRC4_40":
                return new PKCS12PBECipherCore.PBEWithSHA1AndRC4_40();
            case "com.sun.crypto.provider.PKCS12PBECipherCore$PBEWithSHA1AndRC4_128":
                return new PKCS12PBECipherCore.PBEWithSHA1AndRC4_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA1AndAES_128":
                return new PBES2Core.HmacSHA1AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA224AndAES_128":
                return new PBES2Core.HmacSHA224AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA256AndAES_128":
                return new PBES2Core.HmacSHA256AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA384AndAES_128":
                return new PBES2Core.HmacSHA384AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA512AndAES_128":
                return new PBES2Core.HmacSHA512AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA512_224AndAES_128":
                return new PBES2Core.HmacSHA512_224AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA512_256AndAES_128":
                return new PBES2Core.HmacSHA512_256AndAES_128();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA1AndAES_256":
                return new PBES2Core.HmacSHA1AndAES_256();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA224AndAES_256":
                return new PBES2Core.HmacSHA224AndAES_256();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA256AndAES_256":
                return new PBES2Core.HmacSHA256AndAES_256();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA384AndAES_256":
                return new PBES2Core.HmacSHA384AndAES_256();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA512AndAES_256":
                return new PBES2Core.HmacSHA512AndAES_256();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA512_224AndAES_256":
                return new PBES2Core.HmacSHA512_224AndAES_256();
            case "com.sun.crypto.provider.PBES2Core$HmacSHA512_256AndAES_256":
                return new PBES2Core.HmacSHA512_256AndAES_256();
            case "com.sun.crypto.provider.HPKE":
                return new HPKE();
            case "com.sun.crypto.provider.DESKeyGenerator":
                return new DESKeyGenerator();
            case "com.sun.crypto.provider.DESedeKeyGenerator":
                return new DESedeKeyGenerator();
            case "com.sun.crypto.provider.BlowfishKeyGenerator":
                return new BlowfishKeyGenerator();
            case "com.sun.crypto.provider.AESKeyGenerator":
                return new AESKeyGenerator();
            case "com.sun.crypto.provider.KeyGeneratorCore$RC2KeyGenerator":
                return new KeyGeneratorCore.RC2KeyGenerator();
            case "com.sun.crypto.provider.KeyGeneratorCore$ARCFOURKeyGenerator":
                return new KeyGeneratorCore.ARCFOURKeyGenerator();
            case "com.sun.crypto.provider.KeyGeneratorCore$ChaCha20KeyGenerator":
                return new KeyGeneratorCore.ChaCha20KeyGenerator();
            case "com.sun.crypto.provider.HmacMD5KeyGenerator":
                return new HmacMD5KeyGenerator();
            case "com.sun.crypto.provider.HmacSHA1KeyGenerator":
                return new HmacSHA1KeyGenerator();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA224":
                return new KeyGeneratorCore.HmacKG.SHA224();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA256":
                return new KeyGeneratorCore.HmacKG.SHA256();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA384":
                return new KeyGeneratorCore.HmacKG.SHA384();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA512":
                return new KeyGeneratorCore.HmacKG.SHA512();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA512_224":
                return new KeyGeneratorCore.HmacKG.SHA512_224();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA512_256":
                return new KeyGeneratorCore.HmacKG.SHA512_256();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA3_224":
                return new KeyGeneratorCore.HmacKG.SHA3_224();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA3_256":
                return new KeyGeneratorCore.HmacKG.SHA3_256();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA3_384":
                return new KeyGeneratorCore.HmacKG.SHA3_384();
            case "com.sun.crypto.provider.KeyGeneratorCore$HmacKG$SHA3_512":
                return new KeyGeneratorCore.HmacKG.SHA3_512();
            case "com.sun.crypto.provider.DHKeyPairGenerator":
                return new DHKeyPairGenerator();
            case "com.sun.crypto.provider.DHParameterGenerator":
                return new DHParameterGenerator();
            case "com.sun.crypto.provider.DHKeyAgreement":
                return new DHKeyAgreement();
            case "com.sun.crypto.provider.DHParameters":
                return new DHParameters();
            case "com.sun.crypto.provider.DESParameters":
                return new DESParameters();
            case "com.sun.crypto.provider.DESedeParameters":
                return new DESedeParameters();
            case "com.sun.crypto.provider.PBEParameters":
                return new PBEParameters();
            case "com.sun.crypto.provider.PBES2Parameters$General":
                return new PBES2Parameters.General();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA1AndAES_128":
                return new PBES2Parameters.HmacSHA1AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA224AndAES_128":
                return new PBES2Parameters.HmacSHA224AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA256AndAES_128":
                return new PBES2Parameters.HmacSHA256AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA384AndAES_128":
                return new PBES2Parameters.HmacSHA384AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA512AndAES_128":
                return new PBES2Parameters.HmacSHA512AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA512_224AndAES_128":
                return new PBES2Parameters.HmacSHA512_224AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA512_256AndAES_128":
                return new PBES2Parameters.HmacSHA512_256AndAES_128();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA1AndAES_256":
                return new PBES2Parameters.HmacSHA1AndAES_256();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA224AndAES_256":
                return new PBES2Parameters.HmacSHA224AndAES_256();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA256AndAES_256":
                return new PBES2Parameters.HmacSHA256AndAES_256();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA384AndAES_256":
                return new PBES2Parameters.HmacSHA384AndAES_256();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA512AndAES_256":
                return new PBES2Parameters.HmacSHA512AndAES_256();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA512_224AndAES_256":
                return new PBES2Parameters.HmacSHA512_224AndAES_256();
            case "com.sun.crypto.provider.PBES2Parameters$HmacSHA512_256AndAES_256":
                return new PBES2Parameters.HmacSHA512_256AndAES_256();
            case "com.sun.crypto.provider.BlowfishParameters":
                return new BlowfishParameters();
            case "com.sun.crypto.provider.AESParameters":
                return new AESParameters();
            case "com.sun.crypto.provider.RC2Parameters":
                return new RC2Parameters();
            case "com.sun.crypto.provider.OAEPParameters":
                return new OAEPParameters();
            case "com.sun.crypto.provider.ChaCha20Poly1305Parameters":
                return new ChaCha20Poly1305Parameters();
            case "com.sun.crypto.provider.DHKeyFactory":
                return new DHKeyFactory();
            case "com.sun.crypto.provider.DESKeyFactory":
                return new DESKeyFactory();
            case "com.sun.crypto.provider.DESedeKeyFactory":
                return new DESedeKeyFactory();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithMD5AndDES":
                return new PBEKeyFactory.PBEWithMD5AndDES();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithMD5AndTripleDES":
                return new PBEKeyFactory.PBEWithMD5AndTripleDES();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithSHA1AndDESede":
                return new PBEKeyFactory.PBEWithSHA1AndDESede();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithSHA1AndRC2_40":
                return new PBEKeyFactory.PBEWithSHA1AndRC2_40();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithSHA1AndRC2_128":
                return new PBEKeyFactory.PBEWithSHA1AndRC2_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithSHA1AndRC4_40":
                return new PBEKeyFactory.PBEWithSHA1AndRC4_40();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithSHA1AndRC4_128":
                return new PBEKeyFactory.PBEWithSHA1AndRC4_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA1AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA1AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA224AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA224AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA256AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA256AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA384AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA384AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA512AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA512AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA512_224AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA512_224AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA512_256AndAES_128":
                return new PBEKeyFactory.PBEWithHmacSHA512_256AndAES_128();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA1AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA1AndAES_256();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA224AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA224AndAES_256();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA256AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA256AndAES_256();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA384AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA384AndAES_256();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA512AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA512AndAES_256();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA512_224AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA512_224AndAES_256();
            case "com.sun.crypto.provider.PBEKeyFactory$PBEWithHmacSHA512_256AndAES_256":
                return new PBEKeyFactory.PBEWithHmacSHA512_256AndAES_256();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA1":
                return new PBKDF2Core.HmacSHA1();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA224":
                return new PBKDF2Core.HmacSHA224();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA256":
                return new PBKDF2Core.HmacSHA256();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA384":
                return new PBKDF2Core.HmacSHA384();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA512":
                return new PBKDF2Core.HmacSHA512();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA512_224":
                return new PBKDF2Core.HmacSHA512_224();
            case "com.sun.crypto.provider.PBKDF2Core$HmacSHA512_256":
                return new PBKDF2Core.HmacSHA512_256();
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
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA1":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA1();
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA224":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA224();
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA256":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA256();
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA384":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA384();
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA512":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA512();
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA512_224":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA512_224();
            case "com.sun.crypto.provider.HmacPKCS12PBECore$HmacPKCS12PBE_SHA512_256":
                return new HmacPKCS12PBECore.HmacPKCS12PBE_SHA512_256();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA1":
                return new PBMAC1Core.HmacSHA1();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA224":
                return new PBMAC1Core.HmacSHA224();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA256":
                return new PBMAC1Core.HmacSHA256();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA384":
                return new PBMAC1Core.HmacSHA384();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA512":
                return new PBMAC1Core.HmacSHA512();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA512_224":
                return new PBMAC1Core.HmacSHA512_224();
            case "com.sun.crypto.provider.PBMAC1Core$HmacSHA512_256":
                return new PBMAC1Core.HmacSHA512_256();
            case "com.sun.crypto.provider.SslMacCore$SslMacMD5":
                return new SslMacCore.SslMacMD5();
            case "com.sun.crypto.provider.SslMacCore$SslMacSHA1":
                return new SslMacCore.SslMacSHA1();
            case "com.sun.crypto.provider.DHKEM":
                return new DHKEM();
            case "com.sun.crypto.provider.ML_KEM_Impls$K":
                return new ML_KEM_Impls.K();
            case "com.sun.crypto.provider.ML_KEM_Impls$K2":
                return new ML_KEM_Impls.K2();
            case "com.sun.crypto.provider.ML_KEM_Impls$K3":
                return new ML_KEM_Impls.K3();
            case "com.sun.crypto.provider.ML_KEM_Impls$K5":
                return new ML_KEM_Impls.K5();
            case "com.sun.crypto.provider.ML_KEM_Impls$KPG":
                return new ML_KEM_Impls.KPG();
            case "com.sun.crypto.provider.ML_KEM_Impls$KPG2":
                return new ML_KEM_Impls.KPG2();
            case "com.sun.crypto.provider.ML_KEM_Impls$KPG3":
                return new ML_KEM_Impls.KPG3();
            case "com.sun.crypto.provider.ML_KEM_Impls$KPG5":
                return new ML_KEM_Impls.KPG5();
            case "com.sun.crypto.provider.ML_KEM_Impls$KF":
                return new ML_KEM_Impls.KF();
            case "com.sun.crypto.provider.ML_KEM_Impls$KF2":
                return new ML_KEM_Impls.KF2();
            case "com.sun.crypto.provider.ML_KEM_Impls$KF3":
                return new ML_KEM_Impls.KF3();
            case "com.sun.crypto.provider.ML_KEM_Impls$KF5":
                return new ML_KEM_Impls.KF5();
            case "com.sun.crypto.provider.TlsPrfGenerator$V10":
                return new TlsPrfGenerator.V10();
            case "com.sun.crypto.provider.TlsPrfGenerator$V12":
                return new TlsPrfGenerator.V12();
            case "com.sun.crypto.provider.TlsMasterSecretGenerator":
                return new TlsMasterSecretGenerator();
            case "com.sun.crypto.provider.TlsKeyMaterialGenerator":
                return new TlsKeyMaterialGenerator();
            case "com.sun.crypto.provider.TlsRsaPremasterSecretGenerator":
                return new TlsRsaPremasterSecretGenerator();
            default:
                return null;
        }
    }

    /**
     * A new instance of the SunJCE KDF service class className with its KDFParameters (the HKDF
     * classes have no other constructor), or null if it is not one.
     */
    public static Object newKdfInstance(String className, KDFParameters params)
            throws Exception {
        switch (className) {
            case "com.sun.crypto.provider.HKDFKeyDerivation$HKDFSHA256":
                return new HKDFKeyDerivation.HKDFSHA256(params);
            case "com.sun.crypto.provider.HKDFKeyDerivation$HKDFSHA384":
                return new HKDFKeyDerivation.HKDFSHA384(params);
            case "com.sun.crypto.provider.HKDFKeyDerivation$HKDFSHA512":
                return new HKDFKeyDerivation.HKDFSHA512(params);
            default:
                return null;
        }
    }
}
