/*
 * Arbace (overlay/jdk, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"): the access
 * objects java.util.zip and java.util.jar give each other through jdk.internal.access.SharedSecrets
 * in the JDK (JavaUtilZipFileAccess, set by ZipFile's static initializer; JavaUtilJarAccess, set
 * by JarFile's), which jrt's SharedSecrets does not hold: jrt's ZipFile and JarFile
 * (overlay/jdk/java.base/java/util/zip/ZipFile.java, overlay/jdk/variants/JarFile.ae) set and read
 * them here. JarFile extends ZipFile, so ZipFile's is set before JarFile's static initializer
 * reads it; JarFile's is read only for a JarFile, whose class is then initialized.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import jdk.internal.access.JavaUtilJarAccess;
import jdk.internal.access.JavaUtilZipFileAccess;

/** The zip and jar packages' access objects (SharedSecrets' in the JDK). */
public final class ZipAccess {

    private static JavaUtilZipFileAccess zipFileAccess;
    private static JavaUtilJarAccess jarAccess;

    private ZipAccess() {
    }

    /** Called once, by ZipFile's static initializer. */
    public static void setJavaUtilZipFileAccess(JavaUtilZipFileAccess access) {
        zipFileAccess = access;
    }

    /** ZipFile's access object. */
    public static JavaUtilZipFileAccess javaUtilZipFileAccess() {
        return zipFileAccess;
    }

    /** Called once, by JarFile's static initializer. */
    public static void setJavaUtilJarAccess(JavaUtilJarAccess access) {
        jarAccess = access;
    }

    /** JarFile's access object. */
    public static JavaUtilJarAccess javaUtilJarAccess() {
        return jarAccess;
    }
}
