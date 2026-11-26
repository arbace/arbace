/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-CONFORMANCE-SURVEY.md, JCS53 (a); queue 549): the file
 * objects of javac on Arbace's file manager (jdk.internal.jrt.javac.FileManager) as javac's own
 * PathFileObject, so that javac treats them as it treats its JavacFileManager's: a raw diagnostic
 * names one by its short name (RawDiagnosticFormatter), two objects of one file are one (equality
 * by the path, Log's de-duplication), and its characters are decoded by the file manager
 * (BaseFileManager.decode: -encoding, an unmappable character reported). In javac's package, since
 * PathFileObject's inferBinaryName and getSibling are package-private. Written for javac's
 * documented API and PathFileObject's contract (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package com.sun.tools.javac.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/** A PathFileObject whose binary name its file manager gives (the directory and name it was found under). */
public abstract class ArbaceFileObject extends PathFileObject {

    protected ArbaceFileObject(BaseFileManager fileManager, Path path) {
        super(fileManager, path);
    }

    /** The binary name the file was found under, null for an output or a resource. */
    protected abstract String binaryName();

    /** The file's bytes, read as the file manager reads them. */
    protected abstract byte[] content() throws IOException;

    @Override
    String inferBinaryName(Iterable<? extends Path> paths) {
        return binaryName();
    }

    @Override
    PathFileObject getSibling(String basename) {
        throw new UnsupportedOperationException("a sibling of " + this);
    }

    /**
     * javac's decoding (PathFileObject's) once javac's context is the file manager's (its log set:
     * BaseFileManager.setContext), else UTF-8, as before (a file manager used outside a task).
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        if (fileManager.log == null) {
            return new String(content(), "UTF-8");
        }
        return super.getCharContent(ignoreEncodingErrors);
    }
}
