/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JFS52 (b)"): javac on
 * Arbace's file manager (FileManager, JFS5 (b)) as javax.tools' StandardJavaFileManager, what
 * SystemCompiler's getStandardFileManager gives: file objects of files named by File or by path,
 * and the source path, class path and class output set by setLocation. Written for javac's
 * documented javax.tools API (https://github.com/openjdk/jdk26u at baf63fb): Copyright (c) the
 * Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt.javac;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

/** FileManager as a StandardJavaFileManager: no source path, the current directory as class path, no output. */
public final class StandardFileManager extends FileManager implements StandardJavaFileManager {

    public StandardFileManager() {
        super(null, List.of(new File(".")), null);
    }

    /** A file object of a file, its kind by its extension (.java a source, .class a class, .html, else other). */
    public Obj file(File f) {
        String n = f.getName();
        for (JavaFileObject.Kind k : List.of(JavaFileObject.Kind.SOURCE, JavaFileObject.Kind.CLASS, JavaFileObject.Kind.HTML)) {
            if (n.endsWith(k.extension)) {
                return new Obj(f, k, null);
            }
        }
        return new Obj(f, JavaFileObject.Kind.OTHER, null);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
        List<JavaFileObject> out = new ArrayList<>();
        for (File f : files) {
            out.add(file(f));
        }
        return out;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return getJavaFileObjectsFromFiles(Arrays.asList(files));
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        List<JavaFileObject> out = new ArrayList<>();
        for (String n : names) {
            out.add(file(new File(n)));
        }
        return out;
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return getJavaFileObjectsFromStrings(Arrays.asList(names));
    }

    /** The source path, the class path (null: the current directory) and the class output (one directory or null). */
    @Override
    public void setLocation(Location location, Iterable<? extends File> files) {
        List<File> fs = null;
        if (files != null) {
            fs = new ArrayList<>();
            for (File f : files) {
                fs.add(f);
            }
        }
        if (location == StandardLocation.SOURCE_PATH) {
            sourcePath = fs;
        } else if (location == StandardLocation.CLASS_PATH) {
            classPath = fs == null ? List.of(new File(".")) : fs;
        } else if (location == StandardLocation.CLASS_OUTPUT) {
            if (fs != null && fs.size() != 1) {
                throw new IllegalArgumentException("one class output directory, not " + fs);
            }
            classOutput = fs == null ? null : fs.get(0);
        } else {
            throw new IllegalArgumentException("unsupported on Arbace: setLocation " + location);
        }
    }

    @Override
    public Iterable<? extends File> getLocation(Location location) {
        return hasLocation(location) && !(location instanceof ModuleLoc) ? dirs(location) : null;
    }
}
