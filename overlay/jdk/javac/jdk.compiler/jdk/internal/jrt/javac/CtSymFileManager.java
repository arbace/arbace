/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS50 (b)"): the
 * release file manager of javac on Arbace's --release N, ct.sym served as javac's
 * JDKPlatformProvider serves it (PlatformDescriptionImpl.getFileManager: a JavacFileManager over
 * ct.sym's zip file system), read here through java.util.zip.ZipFile (jrt's, JC6b): the sections
 * whose names hold N's character (radix 36) and no "-"; for a release of modules each section's
 * module directories as that module's system module location (--system none, setLocationForModule),
 * for an older one every section's module directories as the platform class path; a .sig file is a
 * class file (SigJavaFileObject). javac's DelegatingJavaFileManager asks it for the locations it
 * has, the compilation's own file manager for the rest. Written for javac's platform API and
 * javax.tools (https://github.com/openjdk/jdk26u at baf63fb): Copyright (c) the Arbace authors;
 * Eclipse Public License 1.0.
 */
package jdk.internal.jrt.javac;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/** ct.sym's sections of one release, as javac's release file manager. */
final class CtSymFileManager implements JavaFileManager {

    /**
     * A file of ct.sym: a .sig as a class file, any other as OTHER; its URI the zip file system's
     * (jar:file:...!/ENTRY, which SimpleJavaFileObject's hierarchical URI cannot be).
     */
    static final class Entry implements JavaFileObject {
        final ZipFile zip;
        final URI base;
        final ZipEntry entry;
        final Kind kind;
        final String binaryName;
        final String module;

        Entry(ZipFile zip, URI base, ZipEntry entry, Kind kind, String binaryName, String module) {
            this.zip = zip;
            this.base = base;
            this.entry = entry;
            this.kind = kind;
            this.binaryName = binaryName;
            this.module = module;
        }

        @Override
        public URI toUri() {
            return URI.create(base + "!/" + entry.getName());
        }

        @Override
        public Kind getKind() {
            return kind;
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            throw new UnsupportedOperationException("ct.sym is read-only");
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return new StringReader(getCharContent(ignoreEncodingErrors).toString());
        }

        @Override
        public Writer openWriter() throws IOException {
            throw new UnsupportedOperationException("ct.sym is read-only");
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public NestingKind getNestingKind() {
            return null;
        }

        @Override
        public Modifier getAccessLevel() {
            return null;
        }

        /** The entry's path in ct.sym's zip file system. */
        @Override
        public String getName() {
            return "/" + entry.getName();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return zip.getInputStream(entry);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            try (InputStream in = openInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), "UTF-8");
            }
        }

        /** SigJavaFileObject's: a class's simple name against NAME.sig. */
        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            if (kind != getKind()) {
                return false;
            }
            String file = simpleName + (kind == Kind.CLASS ? ".sig" : kind.extension);
            String n = entry.getName();
            return n.equals(file) || n.endsWith("/" + file);
        }

        @Override
        public long getLastModified() {
            return 0L;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /** A module's system module location: its sections' directories in ct.sym. */
    static final class ModuleLoc implements Location {
        final String module;
        final List<String> roots = new ArrayList<>();

        ModuleLoc(String module) {
            this.module = module;
        }

        @Override
        public String getName() {
            return StandardLocation.SYSTEM_MODULES.getName() + "[" + module + "]";
        }

        @Override
        public boolean isOutputLocation() {
            return false;
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

    final ZipFile zip;
    final URI base;
    final boolean hasModules;
    /** The modules of a release of modules, by name. */
    final Map<String, ModuleLoc> modules = new TreeMap<>();
    /** The platform class path of an older release: SECTION/MODULE directories. */
    final List<String> classPath = new ArrayList<>();
    /** Each directory's files (no trailing "/"), by name. */
    final Map<String, Map<String, ZipEntry>> files = new HashMap<>();
    /** Each directory's subdirectories. */
    final Map<String, Set<String>> dirs = new HashMap<>();

    CtSymFileManager(File ctSym, String ctSymVersion, boolean hasModules) throws IOException {
        this.zip = new ZipFile(ctSym);
        this.base = URI.create("jar:" + ctSym.getAbsoluteFile().toURI());
        this.hasModules = hasModules;
        Set<String> roots = new TreeSet<>();
        for (Enumeration<? extends ZipEntry> es = zip.entries(); es.hasMoreElements();) {
            ZipEntry e = es.nextElement();
            String n = e.getName();
            int s1 = n.indexOf('/');
            if (s1 <= 0) {
                continue;
            }
            String section = n.substring(0, s1);
            if (!section.contains(ctSymVersion) || section.contains("-")) {
                continue;
            }
            int s2 = n.indexOf('/', s1 + 1);
            if (s2 < 0) {
                continue;
            }
            roots.add(n.substring(0, s2));
            if (e.isDirectory()) {
                continue;
            }
            int last = n.lastIndexOf('/');
            String dir = n.substring(0, last);
            files.computeIfAbsent(dir, k -> new TreeMap<>()).put(n.substring(last + 1), e);
            // the directories above it, up to its root
            for (String d = dir; d.length() > s2; d = d.substring(0, d.lastIndexOf('/'))) {
                String parent = d.substring(0, d.lastIndexOf('/'));
                if (!dirs.computeIfAbsent(parent, k -> new TreeSet<>()).add(d.substring(parent.length() + 1))) {
                    break;
                }
            }
        }
        for (String r : roots) {
            if (hasModules) {
                String m = r.substring(r.indexOf('/') + 1);
                modules.computeIfAbsent(m, ModuleLoc::new).roots.add(r);
            } else {
                classPath.add(r);
            }
        }
    }

    List<String> roots(Location location) {
        if (location instanceof ModuleLoc m && modules.get(m.module) == m) {
            return m.roots;
        }
        if (!hasModules && location == StandardLocation.PLATFORM_CLASS_PATH) {
            return classPath;
        }
        return List.of();
    }

    static String moduleOf(String root) {
        return root.substring(root.indexOf('/') + 1);
    }

    void listDir(String root, String dir, String pkg, Set<JavaFileObject.Kind> kinds, boolean recurse,
            List<JavaFileObject> out) {
        Map<String, ZipEntry> fs = files.get(dir);
        if (fs != null) {
            for (Map.Entry<String, ZipEntry> f : fs.entrySet()) {
                String n = f.getKey();
                JavaFileObject.Kind k;
                String simple;
                if (n.endsWith(".sig")) {
                    k = JavaFileObject.Kind.CLASS;
                    simple = n.substring(0, n.length() - 4);
                } else {
                    k = JavaFileObject.Kind.OTHER;
                    simple = n;
                    for (JavaFileObject.Kind kk : JavaFileObject.Kind.values()) {
                        if (kk != JavaFileObject.Kind.OTHER && n.endsWith(kk.extension)) {
                            k = kk;
                            simple = n.substring(0, n.length() - kk.extension.length());
                            break;
                        }
                    }
                }
                if (kinds.contains(k)) {
                    out.add(new Entry(zip, base, f.getValue(), k, pkg.isEmpty() ? simple : pkg + "." + simple, moduleOf(root)));
                }
            }
        }
        if (recurse) {
            Set<String> ds = dirs.get(dir);
            if (ds != null) {
                for (String d : ds) {
                    listDir(root, dir + "/" + d, pkg.isEmpty() ? d : pkg + "." + d, kinds, true, out);
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
        for (String r : roots(location)) {
            listDir(r, packageName.isEmpty() ? r : r + "/" + packageName.replace('.', '/'), packageName, kinds, recurse, out);
        }
        return out;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        return file instanceof Entry e ? e.binaryName : null;
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        if (a instanceof Entry x && b instanceof Entry y) {
            return x.entry.getName().equals(y.entry.getName());
        }
        return a == b;
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return false;
    }

    /** JDKPlatformProvider's file manager's hasLocation: only the locations it set explicitly. */
    @Override
    public boolean hasLocation(Location location) {
        if (hasModules) {
            return location == StandardLocation.SYSTEM_MODULES
                    || (location instanceof ModuleLoc m && modules.get(m.module) == m);
        }
        return location == StandardLocation.PLATFORM_CLASS_PATH;
    }

    Entry find(Location location, String relative, JavaFileObject.Kind kind, String binaryName) {
        for (String r : roots(location)) {
            String n = r + "/" + relative;
            int last = n.lastIndexOf('/');
            Map<String, ZipEntry> fs = files.get(n.substring(0, last));
            ZipEntry e = fs == null ? null : fs.get(n.substring(last + 1));
            if (e != null) {
                return new Entry(zip, base, e, kind, binaryName, moduleOf(r));
            }
        }
        return null;
    }

    /** A class's NAME.sig as its class file, as JDKPlatformProvider's getJavaFileForInput. */
    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, JavaFileObject.Kind kind) {
        String path = className.replace('.', '/');
        if (kind == JavaFileObject.Kind.CLASS) {
            Entry e = find(location, path + ".sig", kind, className);
            if (e != null) {
                return e;
            }
        }
        return find(location, path + kind.extension, kind, className);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        throw new IOException("ct.sym is read-only: " + location + " " + className);
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName, String relativeName) {
        return find(location, packageName.isEmpty() ? relativeName : packageName.replace('.', '/') + "/" + relativeName,
                JavaFileObject.Kind.OTHER, null);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
        throw new IOException("ct.sym is read-only: " + location + " " + relativeName);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    @Override
    public int isSupportedOption(String option) {
        return -1;
    }

    @Override
    public Location getLocationForModule(Location location, String moduleName) {
        return hasModules && location == StandardLocation.SYSTEM_MODULES ? modules.get(moduleName) : null;
    }

    @Override
    public Location getLocationForModule(Location location, JavaFileObject fo) {
        if (hasModules && location == StandardLocation.SYSTEM_MODULES && fo instanceof Entry e && e.zip == zip) {
            return modules.get(e.module);
        }
        return null;
    }

    @Override
    public String inferModuleName(Location location) {
        return location instanceof ModuleLoc m ? m.module : null;
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) {
        if (!hasModules || location != StandardLocation.SYSTEM_MODULES || modules.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new LinkedHashSet<Location>(modules.values()));
    }

    @Override
    public boolean contains(Location location, FileObject fo) {
        if (!(fo instanceof Entry e) || e.zip != zip) {
            return false;
        }
        for (String r : roots(location)) {
            if (e.entry.getName().startsWith(r + "/")) {
                return true;
            }
        }
        return false;
    }
}
