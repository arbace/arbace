/*
 * Arbace (overlay/jdk/javac/buildtools, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359 stages 2-3
 * (day)"): the tool-world variant of the JDK build's TOOL_GENCLASSLOADERMAP
 * (make/jdk/src/classes/build/tools/module/GenModuleLoaderMap.java, make/modules/java.base/gensrc/
 * GensrcModuleLoaderMap.gmk). The build's class also holds a method that reads a module list as a class resource
 * (Class.getResourceAsStream), which none of its callers reaches and which the interpreter of Java forms cannot
 * analyze on javac's tool executable; this variant does what the tool does without it:
 *
 *   GenModuleLoaderMap -o OUTPUT -boot M[,M]* -platform M[,M]* -native-access M[,M]* SOURCE
 *
 * SOURCE (jdk.internal.module.ModuleLoaderMap's source) is copied line by line into OUTPUT (UTF-8, each line ended by
 * the platform's line separator), a line holding @@BOOT_MODULE_NAMES@@, @@PLATFORM_MODULE_NAMES@@ or
 * @@NATIVE_ACCESS_MODULE_NAMES@@ with that tag replaced by its modules, sorted, joined by "\",\n            \"" (the
 * string the template's array initializers expect). An option may be given more than once; its lists are joined.
 * Written for the JDK build's tool's documented behaviour (https://github.com/openjdk/jdk26u at baf63fb):
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package build.tools.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GenModuleLoaderMap {
    public static void main(String... args) throws Exception {
        List<String> boot = new ArrayList<>();
        List<String> platform = new ArrayList<>();
        List<String> nativeAccess = new ArrayList<>();
        String outfile = null;
        String source = null;
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            if (option.startsWith("-")) {
                String arg = args[++i];
                if (option.equals("-boot")) {
                    Collections.addAll(boot, arg.split(","));
                } else if (option.equals("-platform")) {
                    Collections.addAll(platform, arg.split(","));
                } else if (option.equals("-native-access")) {
                    Collections.addAll(nativeAccess, arg.split(","));
                } else if (option.equals("-o")) {
                    outfile = arg;
                } else {
                    throw new IllegalArgumentException("invalid option: " + option);
                }
            } else {
                source = option;
            }
        }
        if (outfile == null) {
            throw new IllegalArgumentException("-o must be specified");
        }
        if (source == null || !new File(source).exists()) {
            throw new IllegalArgumentException(source + " not exist");
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outfile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("@@BOOT_MODULE_NAMES@@")) {
                    line = line.replace("@@BOOT_MODULE_NAMES@@", names(boot));
                } else if (line.contains("@@PLATFORM_MODULE_NAMES@@")) {
                    line = line.replace("@@PLATFORM_MODULE_NAMES@@", names(platform));
                } else if (line.contains("@@NATIVE_ACCESS_MODULE_NAMES@@")) {
                    line = line.replace("@@NATIVE_ACCESS_MODULE_NAMES@@", names(nativeAccess));
                }
                out.println(line);
            }
        }
    }

    private static String names(List<String> modules) {
        List<String> sorted = new ArrayList<>(modules);
        Collections.sort(sorted);
        return String.join("\",\n            \"", sorted);
    }
}
