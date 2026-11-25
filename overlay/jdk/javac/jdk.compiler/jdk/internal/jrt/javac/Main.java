/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN3"): javac's
 * command line on Arbace. javac's own launcher (com.sun.tools.javac.Main) runs over its
 * JavacFileManager; this one parses the file manager's options itself, serves them by
 * FileManager (JFS5 (b)) and hands the rest to javac's API (JavacTool.getTask), whose
 * diagnostics javac prints as on its command line. Since queue 549 (JCS53 (a)) the file manager is
 * javac's context's, as com.sun.tools.javac.main.Main.compile makes it (BaseFileManager.setContext:
 * its log, options and locations), and -XDexpectKeys is judged as Main.compile judges it. Written for
 * javac's documented API (https://github.com/openjdk/jdk26u at baf63fb): Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt.javac;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;
import jdk.internal.opt.CommandLine;

/**
 * javac on Arbace:
 *
 * <pre>
 * Main [--platform DIR]... [-d DIR] [-sourcepath PATH] [-cp PATH] [--patch-module M=PATH]...
 *      [--module-source-path PATH] [--system none] [--module-path PATH] [-encoding CHARSET]
 *      [JAVAC-OPTION...] [@ARGFILE...] FILE.java...
 * </pre>
 *
 * --platform DIR serves each module directory of DIR (one holding a module-info.class) as a
 * system module, as an exploded JDK's jdk/modules; -d, -sourcepath (--source-path), -cp
 * (-classpath, --class-path; default the current directory, as javac's), --patch-module and
 * --module-source-path (javac's module-pattern form) are the file manager's (FileManager),
 * --system none and an empty --module-path are accepted as what the file manager does anyway, a
 * --module-path of elements that do not exist is BaseFileManager's (its Locations: each missing
 * element warned of, path.element.not.found, as javac's command line warns; queue 549b, 549-3 (b)),
 * one naming an existing file or directory refused (no jar file system, no module path served), and
 * -encoding is the file manager's charset (BaseFileManager's, javac's API hands it the option); every
 * other option is javac's (JavacTool.getTask's, e.g.
 * -g:none, -source, --enable-preview, -XD...). @ARGFILE expands as on javac's command line
 * (jdk.internal.opt.CommandLine). A file named twice (by any spelling of its path) is compiled once,
 * under its first name, as by javac's command line. The exit status is javac's: 0 when the compilation succeeds,
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

    /** Does module path p name only elements that do not exist (javac then warns of each, and serves none)? */
    static boolean missingOnly(String p) {
        for (String s : p.split(File.pathSeparator, -1)) {
            if (s.isEmpty() || new File(s).exists()) {
                return false;
            }
        }
        return true;
    }

    /** Option.SOURCEFILE's class name: a legal type name, or MODULE/TYPE. */
    static boolean className(String s) {
        int sep = s.indexOf('/');
        return sep != -1 ? SourceVersion.isName(s.substring(0, sep)) && SourceVersion.isName(s.substring(sep + 1))
                : SourceVersion.isName(s);
    }

    /** FSInfo.getCanonicalFile's path: the real one, else the absolute. */
    static String canonical(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
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
        String modulePath = null;
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
                case "--system", "--module-path" -> {
                    String v = args.get(++i);
                    boolean as = a.equals("--system") ? v.equals("none") : v.isEmpty() || missingOnly(v);
                    if (as && a.equals("--module-path") && !v.isEmpty()) {
                        modulePath = v;
                    }
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
        // a file named twice, by any spelling, compiled once under its first name, as javac's command
        // line: Arguments keeps its files in a set, made file objects whose equality is the canonical
        // path (JavacFileManager.getJavaFileObjectsFromPaths, FSInfo.getCanonicalFile) in a set too;
        // javac's API cannot do it here, it wraps each of a client's file objects apart (ClientCodeWrapper)
        List<JavaFileObject> units = new ArrayList<>();
        Set<String> named = new HashSet<>();
        // a name that is not a .java file but a type's (or MODULE/TYPE), a class for annotation processing, as
        // javac's Option.SOURCEFILE takes it (helper.addClassName; queue 575, @compile/process's class names)
        List<String> classNames = new ArrayList<>();
        for (String f : files) {
            if (!f.endsWith(".java") && className(f)) {
                classNames.add(f);
                continue;
            }
            File file = new File(f);
            if (named.add(canonical(file))) {
                units.add(fm.source(file));
            }
        }
        if (units.isEmpty() && classNames.isEmpty()) {
            err.println("error: no source files");
            err.flush();
            return 2;
        }
        Boolean ok;
        Log log;
        JavaCompiler comp;
        try {
            JavacTaskImpl task = (JavacTaskImpl) JavacTool.create().getTask(err, fm, null, opts, classNames.isEmpty() ? null : classNames, units);
            // javac's Main.compile: the file manager given the context once the options are
            // processed ("reinit with options"), before they are validated
            Context context = task.getContext();
            fm.setContext(context);
            // javac's Main.compile hands the file manager its options once it has the context
            // (BaseFileManager.handleOptions): a module path of missing elements, each warned of
            if (modulePath != null) {
                fm.handleOption(Option.MODULE_PATH, modulePath);
            }
            log = Log.instance(context);
            ok = task.call();
            // the context's compiler (the task's cleanup closed it; its counts' printing needs the log alone)
            comp = JavaCompiler.instance(context);
        } catch (IllegalArgumentException | IllegalStateException e) {
            err.println("error: " + e.getMessage());
            err.flush();
            return 2;
        }
        // an error of the task's preparation (a plug-in not found, -Werror over Arguments' warnings):
        // javac's API task returns before JavaCompiler.compile, whose end prints the counts; Main.compile
        // goes on into it (the files are parsed, nothing more after the error), so its counts are printed
        if (!ok && !fm.sourceRead) {
            comp.printCount("error", comp.errorCount());
            comp.printCount("warn", comp.warningCount());
        }
        // -XDexpectKeys=KEY,...: Main.compile's verdict over the compilation's
        if (log.expectDiagKeys != null) {
            if (log.expectDiagKeys.isEmpty()) {
                log.printRawLines("all expected diagnostics found");
                ok = true;
            } else {
                log.printRawLines("expected diagnostic keys not found: " + log.expectDiagKeys);
                ok = false;
            }
        }
        log.flush();
        err.flush();
        return ok ? 0 : 1;
    }

    public static int run(String[] argv) throws IOException {
        return run(argv, new PrintWriter(System.err, true));
    }

    /**
     * javac's command line against the platform javax.tools' system compiler attributes against
     * (SystemCompiler's property jdk.internal.jrt.javac.platform, each of its directories a
     * --platform), its diagnostics to err: what com.sun.tools.javac.Main's compile methods do on
     * javac's tool (their variant, overlay/jdk/javac/variants/JavacMain.ae; F6,
     * doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: F6"). An I/O error is a bad command line's
     * status, 2, its message on err.
     */
    public static int runOnPlatform(String[] argv, PrintWriter err) {
        List<String> args = new ArrayList<>();
        String p = System.getProperty(SystemCompiler.PLATFORM);
        if (p != null && !p.isEmpty()) {
            for (String d : p.split(File.pathSeparator)) {
                args.add("--platform");
                args.add(d);
            }
        }
        for (String a : argv) {
            args.add(a);
        }
        try {
            return run(args.toArray(new String[0]), err);
        } catch (IOException e) {
            err.println("error: " + e.getMessage());
            err.flush();
            return 2;
        }
    }

    public static void main(String[] args) throws IOException {
        System.exit(run(args));
    }
}
