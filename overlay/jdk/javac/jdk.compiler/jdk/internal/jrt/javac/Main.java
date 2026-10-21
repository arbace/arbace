/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN3"): javac's
 * command line on Arbace. javac's own launcher (com.sun.tools.javac.Main) runs over its
 * JavacFileManager; this one parses the file manager's options itself, serves them by
 * FileManager (JFS5 (b)) and hands the rest to javac's API (JavacTool.getTask), whose
 * diagnostics javac prints as on its command line. Written for javac's documented API
 * (https://github.com/openjdk/jdk26u at baf63fb): Copyright (c) the Arbace authors; Eclipse
 * Public License 1.0.
 */
package jdk.internal.jrt.javac;

import com.sun.tools.javac.api.JavacTool;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.tools.JavaFileObject;
import jdk.internal.opt.CommandLine;

/**
 * javac on Arbace:
 *
 * <pre>
 * Main [--platform DIR]... [-d DIR] [-sourcepath PATH] [-cp PATH] [--patch-module M=PATH]...
 *      [--module-source-path PATH] [--system none] [--module-path ""] [-encoding utf-8]
 *      [JAVAC-OPTION...] [@ARGFILE...] FILE.java...
 * </pre>
 *
 * --platform DIR serves each module directory of DIR (one holding a module-info.class) as a
 * system module, as an exploded JDK's jdk/modules; -d, -sourcepath (--source-path), -cp
 * (-classpath, --class-path; default the current directory, as javac's), --patch-module and
 * --module-source-path (javac's module-pattern form) are the file manager's (FileManager), and
 * --system none, an empty --module-path and -encoding utf-8 (the files are read as UTF-8) are
 * accepted as what the file manager does anyway; every other option is javac's (JavacTool.getTask's, e.g.
 * -g:none, -source, --enable-preview, -XD...). @ARGFILE expands as on javac's command line
 * (jdk.internal.opt.CommandLine). The exit status is javac's: 0 when the compilation succeeds,
 * 1 when it reports errors, 2 for a bad command line.
 */
public final class Main {
    /** javac's options that take an argument (the API's, the file manager's are parsed here). */
    static final Set<String> WITH_ARG = Set.of("-source", "--source", "-target", "--target", "--release",
            "-encoding", "-Xmaxerrs", "-Xmaxwarns", "--add-exports", "--add-reads", "--add-modules",
            "--limit-modules", "--module-version", "-processor", "--default-module-for-created-files",
            "-profile");

    static List<File> path(String p) {
        List<File> fs = new ArrayList<>();
        for (String s : p.split(File.pathSeparator, -1)) {
            fs.add(new File(s.isEmpty() ? "." : s));
        }
        return fs;
    }

    static List<String> expand(String[] args) throws IOException {
        List<String> out = new ArrayList<>();
        for (String a : args) {
            if (a.startsWith("@") && !a.startsWith("@@")) {
                try (InputStream in = new FileInputStream(a.substring(1))) {
                    CommandLine.loadCmdFile(in, out);
                }
            } else {
                out.add(a.startsWith("@@") ? a.substring(1) : a);
            }
        }
        return out;
    }

    /** javac's command line, its diagnostics to err; the exit status. */
    public static int run(String[] argv, PrintWriter err) throws IOException {
        List<String> args;
        try {
            args = expand(argv);
        } catch (IOException e) {
            err.println("error: " + e.getMessage());
            err.flush();
            return 2;
        }
        List<File> platforms = new ArrayList<>();
        List<File> sourcePath = null;
        List<File> classPath = null;
        File out = null;
        List<String[]> patches = new ArrayList<>();
        String moduleSourcePath = null;
        List<String> opts = new ArrayList<>();
        List<String> files = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            String a = args.get(i);
            boolean more = i + 1 < args.size();
            switch (a) {
                case "--platform" -> platforms.add(new File(args.get(++i)));
                case "-d" -> out = new File(args.get(++i));
                case "-sourcepath", "--source-path" -> sourcePath = path(args.get(++i));
                case "-cp", "-classpath", "--class-path" -> classPath = path(args.get(++i));
                case "--module-source-path" -> moduleSourcePath = args.get(++i);
                case "--system", "--module-path", "-encoding" -> {
                    String v = args.get(++i);
                    boolean as = a.equals("--system") ? v.equals("none")
                            : a.equals("--module-path") ? v.isEmpty()
                            : v.equalsIgnoreCase("utf-8") || v.equalsIgnoreCase("utf8");
                    if (!as) {
                        err.println("error: unsupported on Arbace: " + a + " " + v);
                        err.flush();
                        return 2;
                    }
                }
                case "--patch-module" -> {
                    String p = args.get(++i);
                    int eq = p.indexOf('=');
                    if (eq <= 0) {
                        err.println("error: bad value for --patch-module: " + p);
                        err.flush();
                        return 2;
                    }
                    patches.add(new String[] {p.substring(0, eq), p.substring(eq + 1)});
                }
                default -> {
                    if (a.startsWith("-")) {
                        opts.add(a);
                        if (WITH_ARG.contains(a) && more) {
                            opts.add(args.get(++i));
                        }
                    } else {
                        files.add(a);
                    }
                }
            }
        }
        FileManager fm = new FileManager(sourcePath, classPath == null ? path(".") : classPath, out);
        for (File p : platforms) {
            fm.addPlatform(p);
        }
        for (String[] p : patches) {
            fm.addPatch(p[0], path(p[1]));
        }
        if (moduleSourcePath != null) {
            fm.setModuleSourcePath(moduleSourcePath);
        }
        List<JavaFileObject> units = new ArrayList<>();
        for (String f : files) {
            units.add(fm.source(new File(f)));
        }
        if (units.isEmpty()) {
            err.println("error: no source files");
            err.flush();
            return 2;
        }
        Boolean ok;
        try {
            ok = JavacTool.create().getTask(err, fm, null, opts, null, units).call();
        } catch (IllegalArgumentException | IllegalStateException e) {
            err.println("error: " + e.getMessage());
            err.flush();
            return 2;
        }
        err.flush();
        return ok ? 0 : 1;
    }

    public static int run(String[] argv) throws IOException {
        return run(argv, new PrintWriter(System.err, true));
    }

    public static void main(String[] args) throws IOException {
        System.exit(run(args));
    }
}
