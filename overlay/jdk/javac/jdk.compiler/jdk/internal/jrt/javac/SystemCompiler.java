/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JFS52 (b)"): the
 * system Java compiler of javac's tool executable, what javax.tools.ToolProvider's
 * getSystemJavaCompiler returns there (its tool-world variant, overlay/jdk/javac/variants). javac's
 * JavacTool, whose standard file manager (JavacFileManager) reads through java.nio.file's file
 * systems, which the Go build does not have: its tasks are JavacTool's, its standard file manager
 * javac on Arbace's (StandardFileManager, JFS5 (b)), its command line javac on Arbace's (Main).
 * For the JDK build's tools that compile through the API (make/langtools/tools/flagsgenerator).
 * Written for javac's documented javax.tools API (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt.javac;

import com.sun.tools.javac.api.JavacTool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * javac as javax.tools' JavaCompiler on Arbace. The platform of its standard file manager (the
 * JDK's is its own image, java.home's) is the system property jdk.internal.jrt.javac.platform:
 * directories separated by the path separator, each served as Main's --platform DIR (its module
 * directories as system modules); unset, none.
 */
public final class SystemCompiler implements JavaCompiler {
    /** The system property naming the platform of the standard file manager. */
    public static final String PLATFORM = "jdk.internal.jrt.javac.platform";

    final JavacTool javac = JavacTool.create();

    @Override
    public CompilationTask getTask(Writer out, JavaFileManager fileManager,
            DiagnosticListener<? super JavaFileObject> diagnosticListener, Iterable<String> options,
            Iterable<String> classes, Iterable<? extends JavaFileObject> compilationUnits) {
        return javac.getTask(out, fileManager == null ? getStandardFileManager(diagnosticListener, null, null) : fileManager,
                diagnosticListener, options, classes, compilationUnits);
    }

    /** javac on Arbace's file manager: no source path, the class path the current directory, no output, the platform. */
    @Override
    public StandardFileManager getStandardFileManager(DiagnosticListener<? super JavaFileObject> diagnosticListener,
            Locale locale, Charset charset) {
        StandardFileManager fm = new StandardFileManager();
        String p = System.getProperty(PLATFORM);
        if (p != null && !p.isEmpty()) {
            for (String d : p.split(File.pathSeparator)) {
                try {
                    fm.addPlatform(new File(d));
                } catch (IOException e) {
                    throw new IllegalStateException(PLATFORM + ": " + e.getMessage());
                }
            }
        }
        return fm;
    }

    /** javac on Arbace's command line (Main), its diagnostics to err. */
    @Override
    public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
        PrintWriter w = new PrintWriter(err == null ? System.err : err, true);
        try {
            return Main.run(arguments, w);
        } catch (IOException e) {
            w.println("error: " + e.getMessage());
            w.flush();
            return 2;
        }
    }

    @Override
    public Set<SourceVersion> getSourceVersions() {
        return javac.getSourceVersions();
    }

    @Override
    public int isSupportedOption(String option) {
        return javac.isSupportedOption(option);
    }
}
