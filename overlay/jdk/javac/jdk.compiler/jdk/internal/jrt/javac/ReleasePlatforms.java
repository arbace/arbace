/*
 * Arbace (overlay/jdk/javac, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS50 (b)"): javac
 * on Arbace's --release N. javac's own provider (com.sun.tools.javac.platform.JDKPlatformProvider)
 * opens java.home's lib/ct.sym as a zip file system and serves it by a JavacFileManager, neither of
 * which the Go build has; this one finds ct.sym in the platform's directories (DIR/ct.sym, beside the
 * module directories of a --platform DIR of javac on Arbace's file manager, FileManager.addPlatform;
 * bin/javac-vendor platform puts the pinned ct.sym there, jdk/javac/ct-sym.sha256), reads it through java.util.zip.ZipFile and
 * serves it as JDKPlatformProvider does (CtSymFileManager): the releases are the targets its
 * sections name, a release of modules its sections' module directories as the system modules, an
 * older one its sections as the platform class path. Without a ct.sym the platform's own release
 * alone, -source/-target N with the platform's modules as the system modules (JCS50 (b)'s first
 * step). The service of javac's tool world's java.util.ServiceLoader
 * (overlay/jdk/javac/variants/ServiceLoader.ae). Written for javac's platform API
 * (https://github.com/openjdk/jdk26u at baf63fb, src/jdk.compiler/share/classes/com/sun/tools/javac/platform):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt.javac;

import com.sun.source.util.Plugin;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.platform.PlatformProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.processing.Processor;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

/** The platform provider of javac on Arbace: the releases of the platform's ct.sym. */
public final class ReleasePlatforms implements PlatformProvider {

    /** The platforms' directories, as FileManager.addPlatform registers them, in order. */
    static final List<File> PLATFORMS = new ArrayList<>();

    /** A platform directory, registered by FileManager.addPlatform. */
    static synchronized void platform(File dir) {
        File d = dir.getAbsoluteFile();
        if (!PLATFORMS.contains(d)) {
            PLATFORMS.add(d);
        }
    }

    /** The first platform directory's ct.sym (java.home's lib/ct.sym for javac's provider), or null. */
    static synchronized File ctSym() {
        for (File d : PLATFORMS) {
            File f = new File(d, "ct.sym");
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    /** JDKPlatformProvider's NUMERICAL_COMPARATOR. */
    static final Comparator<String> NUMERICAL = (s1, s2) -> {
        int i1;
        try {
            i1 = Integer.parseInt(s1);
        } catch (NumberFormatException ex) {
            i1 = Integer.MAX_VALUE;
        }
        int i2;
        try {
            i2 = Integer.parseInt(s2);
        } catch (NumberFormatException ex) {
            i2 = Integer.MAX_VALUE;
        }
        return i1 != i2 ? i1 - i2 : s1.compareTo(s2);
    };

    static String targetNumericVersion(Target target) {
        return Integer.toString(target.ordinal() - Target.JDK1_1.ordinal() + 1);
    }

    /** The platform's own release: javac's default target's. */
    static String current() {
        return targetNumericVersion(Target.DEFAULT);
    }

    /**
     * JDKPlatformProvider's SUPPORTED_JAVA_PLATFORM_VERSIONS: each character of each section's name
     * (but those with a "-") a target in radix 36; without a ct.sym, the platform's own release.
     */
    static Set<String> supported(File ctSym) {
        Set<String> vs = new TreeSet<>(NUMERICAL);
        if (ctSym == null) {
            vs.add(current());
            return vs;
        }
        Set<String> sections = new TreeSet<>();
        try (ZipFile z = new ZipFile(ctSym)) {
            for (Enumeration<? extends ZipEntry> es = z.entries(); es.hasMoreElements();) {
                String n = es.nextElement().getName();
                int s = n.indexOf('/');
                if (s > 0) {
                    sections.add(n.substring(0, s));
                }
            }
        } catch (IOException ex) {
            return vs;
        }
        for (String section : sections) {
            if (section.contains("-")) {
                continue;
            }
            for (char ver : section.toCharArray()) {
                Target t = Target.lookup("" + Integer.parseInt(Character.toString(ver), Character.MAX_RADIX));
                if (t != null) {
                    vs.add(targetNumericVersion(t));
                }
            }
        }
        return vs;
    }

    @Override
    public Iterable<String> getSupportedPlatformNames() {
        return supported(ctSym());
    }

    @Override
    public PlatformDescription getPlatform(String platformName, String options) throws PlatformNotSupported {
        File ctSym = ctSym();
        if (!supported(ctSym).contains(platformName)) {
            throw new PlatformNotSupported();
        }
        return new Description(platformName, ctSym);
    }

    /** JDKPlatformProvider's PlatformDescriptionImpl over ct.sym, or the platform's own modules. */
    static final class Description implements PlatformDescription {
        final String sourceVersion;
        final File ctSym;
        final List<JavaFileManager> opened = new ArrayList<>();

        Description(String sourceVersion, File ctSym) {
            this.sourceVersion = sourceVersion;
            this.ctSym = ctSym;
        }

        @Override
        public JavaFileManager getFileManager() {
            JavaFileManager fm;
            try {
                if (ctSym != null) {
                    String ctSymVersion = Integer.toString(Integer.parseInt(sourceVersion), Character.MAX_RADIX)
                            .toUpperCase(java.util.Locale.ROOT);
                    boolean hasModules = Feature.MODULES.allowedInSource(Source.lookup(sourceVersion));
                    fm = new CtSymFileManager(ctSym, ctSymVersion, hasModules);
                } else if (sourceVersion.equals(current())) {
                    fm = new OwnModules();
                } else {
                    throw new IllegalStateException("Cannot find ct.sym!");
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
            opened.add(fm);
            return fm;
        }

        @Override
        public String getSourceVersion() {
            return sourceVersion;
        }

        @Override
        public String getTargetVersion() {
            return sourceVersion;
        }

        @Override
        public List<PluginInfo<Processor>> getAnnotationProcessors() {
            return Collections.emptyList();
        }

        @Override
        public List<PluginInfo<Plugin>> getPlugins() {
            return Collections.emptyList();
        }

        @Override
        public List<String> getAdditionalOptions() {
            return Collections.emptyList();
        }

        @Override
        public void close() throws IOException {
            for (JavaFileManager fm : opened) {
                fm.close();
            }
            opened.clear();
        }
    }

    /**
     * The platform's own release without a ct.sym (JCS50 (b)'s first step): the platform's modules
     * as the system modules, the only location javac's release file manager answers for
     * (DelegatingJavaFileManager asks it first), every other the compilation's own file manager's.
     */
    static final class OwnModules extends ForwardingJavaFileManager<FileManager> {
        OwnModules() throws IOException {
            super(new FileManager(null, List.of(), null));
            List<File> ps;
            synchronized (ReleasePlatforms.class) {
                ps = new ArrayList<>(PLATFORMS);
            }
            for (File p : ps) {
                fileManager.addPlatform(p);
            }
        }

        @Override
        public boolean hasLocation(Location location) {
            return location == StandardLocation.SYSTEM_MODULES
                    || (location instanceof FileManager.ModuleLoc m && fileManager.system.get(m.module) == m);
        }
    }
}
