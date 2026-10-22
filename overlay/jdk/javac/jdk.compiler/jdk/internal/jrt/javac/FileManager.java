/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN3", JFS5 (b)):
 * the file manager of javac on Arbace. javac's own JavacFileManager reads through java.nio.file's
 * file systems, jrt:/ and zip, which the Go build does not have; this one serves javac's
 * locations from directories through java.io.File: the source path, the class path, the class
 * output (-d), the platform's system modules (exploded module directories, as the JDK build's
 * jdk/modules holds them), --patch-module's patches and --module-source-path's modules (with the
 * class output's per module: the JDK build's multi-module compilations), module-oriented as
 * javac's Modules, ModuleFinder and ClassWriter ask. It decides only where files come from and go to, not what javac makes of
 * them. Since JFS52 (b) StandardFileManager extends it, the standard file manager of javac's
 * JavaCompiler on Arbace (SystemCompiler). Written for javac's documented javax.tools API (https://github.com/openjdk/jdk26u at
 * baf63fb): Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt.javac;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;

/** javac's locations as directories (JFS5 (b)): no jrt:/, no zip, no java.nio.file. */
public class FileManager implements JavaFileManager {

    /** A file of a directory, with the binary name it was found under (null for output and resources). */
    public static final class Obj extends SimpleJavaFileObject {
        final File file;
        final String path;
        final String binaryName;

        Obj(File file, Kind kind, String binaryName) {
            super(file.getAbsoluteFile().toURI(), kind);
            this.file = file;
            this.path = file.getAbsolutePath();
            this.binaryName = binaryName;
        }

        @Override
        public String getName() {
            return file.getPath();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
                throw new IOException("cannot create the directory " + dir);
            }
            return new FileOutputStream(file);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return new String(bytes(file), "UTF-8");
        }

        @Override
        public long getLastModified() {
            return file.lastModified();
        }

        @Override
        public String toString() {
            return file.getPath();
        }
    }

    /**
     * A module's location: a system module, a module's patch, a module of the module source path
     * or a module's class output, one or more directories.
     */
    static final class ModuleLoc implements Location {
        final Location outer;
        final String module;
        final List<File> dirs;

        ModuleLoc(Location outer, String module, List<File> dirs) {
            this.outer = outer;
            this.module = module;
            this.dirs = dirs;
        }

        @Override
        public String getName() {
            return outer.getName() + "[" + module + "]";
        }

        @Override
        public boolean isOutputLocation() {
            return outer.isOutputLocation();
        }

        @Override
        public boolean isModuleOrientedLocation() {
            return false;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    static byte[] bytes(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    /** Set by the constructor, or by StandardFileManager's setLocation. */
    List<File> sourcePath;
    List<File> classPath;
    File classOutput;
    final Map<String, ModuleLoc> system = new LinkedHashMap<>();
    final Map<String, ModuleLoc> patches = new LinkedHashMap<>();
    final Map<String, ModuleLoc> sourceModules = new LinkedHashMap<>();
    final Map<String, ModuleLoc> outputModules = new LinkedHashMap<>();
    boolean multiModule;

    /**
     * sourcePath: the source roots, null when javac is given none (it then looks for sources on
     * the class path, as with javac's own file manager); classPath: the class roots;
     * classOutput: -d, or null.
     */
    public FileManager(List<File> sourcePath, List<File> classPath, File classOutput) {
        this.sourcePath = sourcePath;
        this.classPath = classPath;
        this.classOutput = classOutput;
    }

    /** Every directory of platform holding a module-info.class, a system module, in name order. */
    public void addPlatform(File platform) throws IOException {
        File[] fs = platform.listFiles();
        if (fs == null) {
            throw new IOException("no platform directory " + platform);
        }
        Arrays.sort(fs);
        for (File d : fs) {
            if (new File(d, "module-info.class").isFile() && !system.containsKey(d.getName())) {
                system.put(d.getName(), new ModuleLoc(StandardLocation.SYSTEM_MODULES, d.getName(), List.of(d)));
            }
        }
    }

    /** --patch-module MODULE=DIR[:DIR...] */
    public void addPatch(String module, List<File> dirs) {
        patches.put(module, new ModuleLoc(StandardLocation.PATCH_MODULE_PATH, module, dirs));
    }

    /**
     * --module-source-path, javac's module-pattern form: entries separated by the path
     * separator, each a directory with * standing for a module's name (P/* or P/* /S: each
     * directory M of P is module M, its sources in P/M/S when that is a directory), the modules'
     * directories in the order of the entries.
     */
    public void setModuleSourcePath(String path) throws IOException {
        multiModule = true;
        for (String e : path.split(File.pathSeparator)) {
            int star = e.indexOf('*');
            if (star < 0 || e.indexOf('*', star + 1) >= 0 || (star > 0 && e.charAt(star - 1) != File.separatorChar)
                    || (star + 1 < e.length() && e.charAt(star + 1) != File.separatorChar)) {
                throw new IOException("unsupported --module-source-path entry " + e);
            }
            File parent = new File(star == 0 ? "." : e.substring(0, star));
            String suffix = star + 1 < e.length() ? e.substring(star + 2) : "";
            File[] ms = parent.listFiles();
            if (ms == null) {
                continue;
            }
            Arrays.sort(ms);
            for (File m : ms) {
                File d = suffix.isEmpty() ? m : new File(m, suffix);
                if (d.isDirectory()) {
                    ModuleLoc l = sourceModules.get(m.getName());
                    if (l == null) {
                        l = new ModuleLoc(StandardLocation.MODULE_SOURCE_PATH, m.getName(), new ArrayList<>());
                        sourceModules.put(m.getName(), l);
                    }
                    l.dirs.add(d);
                }
            }
        }
    }

    ModuleLoc outputModule(String module) {
        ModuleLoc l = outputModules.get(module);
        if (l == null && classOutput != null) {
            l = new ModuleLoc(StandardLocation.CLASS_OUTPUT, module, List.of(new File(classOutput, module)));
            outputModules.put(module, l);
        }
        return l;
    }

    /** The class output file of a location: -d, or a module's directory below it. */
    File outputDir(Location location) {
        if (location == StandardLocation.CLASS_OUTPUT && classOutput != null && !multiModule) {
            return classOutput;
        }
        if (location instanceof ModuleLoc m && m.outer == StandardLocation.CLASS_OUTPUT) {
            return m.dirs.get(0);
        }
        return null;
    }

    public Obj source(File f) {
        return new Obj(f, JavaFileObject.Kind.SOURCE, null);
    }

    List<File> dirs(Location location) {
        if (location instanceof ModuleLoc m) {
            return m.dirs;
        }
        if (location == StandardLocation.SOURCE_PATH) {
            return sourcePath == null ? List.of() : sourcePath;
        }
        if (location == StandardLocation.CLASS_PATH) {
            return classPath;
        }
        if (location == StandardLocation.CLASS_OUTPUT) {
            return classOutput == null || multiModule ? List.of() : List.of(classOutput);
        }
        return List.of();
    }

    static void listDir(File root, String pkg, Set<JavaFileObject.Kind> kinds, boolean recurse, List<JavaFileObject> out) {
        File dir = pkg.isEmpty() ? root : new File(root, pkg.replace('.', '/'));
        File[] fs = dir.listFiles();
        if (fs == null) {
            return;
        }
        Arrays.sort(fs);
        for (File f : fs) {
            String n = f.getName();
            if (f.isDirectory()) {
                if (recurse) {
                    listDir(root, pkg.isEmpty() ? n : pkg + "." + n, kinds, true, out);
                }
                continue;
            }
            for (JavaFileObject.Kind k : kinds) {
                if (k != JavaFileObject.Kind.OTHER && n.endsWith(k.extension)) {
                    String simple = n.substring(0, n.length() - k.extension.length());
                    out.add(new Obj(f, k, pkg.isEmpty() ? simple : pkg + "." + simple));
                    break;
                }
            }
        }
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return null;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) {
        List<JavaFileObject> out = new ArrayList<>();
        for (File r : dirs(location)) {
            listDir(r, packageName, kinds, recurse, out);
        }
        return out;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        return file instanceof Obj o ? o.binaryName : null;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        if (a instanceof Obj x && b instanceof Obj y) {
            return x.path.equals(y.path);
        }
        return a == b;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return false;
    }

    @Override
    public boolean hasLocation(Location location) {
        if (location instanceof ModuleLoc) {
            return true;
        }
        if (location == StandardLocation.SOURCE_PATH) {
            return sourcePath != null;
        }
        if (location == StandardLocation.CLASS_PATH) {
            return true;
        }
        if (location == StandardLocation.CLASS_OUTPUT) {
            return classOutput != null;
        }
        if (location == StandardLocation.SYSTEM_MODULES) {
            return !system.isEmpty();
        }
        if (location == StandardLocation.PATCH_MODULE_PATH) {
            return !patches.isEmpty();
        }
        if (location == StandardLocation.MODULE_SOURCE_PATH) {
            return multiModule;
        }
        return false;
    }

    File find(Location location, String relative) {
        for (File r : dirs(location)) {
            File f = new File(r, relative);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) {
        File f = find(location, className.replace('.', '/') + kind.extension);
        return f == null ? null : new Obj(f, kind, className);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        File dir = outputDir(location);
        if (dir == null) {
            throw new IOException("no output location " + location + " for " + className);
        }
        return new Obj(new File(dir, className.replace('.', '/') + kind.extension), kind, null);
    }

    static String relative(String packageName, String relativeName) {
        return packageName.isEmpty() ? relativeName : packageName.replace('.', '/') + "/" + relativeName;
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) {
        File f = find(location, relative(packageName, relativeName));
        return f == null ? null : new Obj(f, JavaFileObject.Kind.OTHER, null);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        File dir = outputDir(location);
        if (dir == null) {
            throw new IOException("no output location " + location + " for " + relativeName);
        }
        return new Obj(new File(dir, relative(packageName, relativeName)), JavaFileObject.Kind.OTHER, null);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public int isSupportedOption(String option) {
        return -1;
    }

    @Override
    public Location getLocationForModule(Location location, String moduleName) {
        if (location == StandardLocation.SYSTEM_MODULES) {
            return system.get(moduleName);
        }
        if (location == StandardLocation.PATCH_MODULE_PATH) {
            return patches.get(moduleName);
        }
        if (location == StandardLocation.MODULE_SOURCE_PATH) {
            return sourceModules.get(moduleName);
        }
        if (location == StandardLocation.CLASS_OUTPUT && multiModule) {
            return outputModule(moduleName);
        }
        return null;
    }

    @Override
    public Location getLocationForModule(Location location, JavaFileObject fo) {
        Map<String, ModuleLoc> ms = location == StandardLocation.PATCH_MODULE_PATH ? patches
                : location == StandardLocation.MODULE_SOURCE_PATH ? sourceModules
                : location == StandardLocation.CLASS_OUTPUT && multiModule ? outputModules : null;
        if (ms != null) {
            for (ModuleLoc m : ms.values()) {
                if (contains(m, fo)) {
                    return m;
                }
            }
        }
        return null;
    }

    @Override
    public String inferModuleName(Location location) {
        return location instanceof ModuleLoc m ? m.module : null;
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) {
        Map<String, ModuleLoc> ms = location == StandardLocation.SYSTEM_MODULES ? system
                : location == StandardLocation.PATCH_MODULE_PATH ? patches
                : location == StandardLocation.MODULE_SOURCE_PATH ? sourceModules : null;
        if (ms == null || ms.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new LinkedHashSet<Location>(ms.values()));
    }

    @Override
    public boolean contains(Location location, FileObject fo) {
        if (!(fo instanceof Obj o)) {
            return false;
        }
        for (File r : dirs(location)) {
            String p = r.getAbsolutePath();
            if (o.path.startsWith(p.endsWith(File.separator) ? p : p + File.separator)) {
                return true;
            }
        }
        return false;
    }
}
