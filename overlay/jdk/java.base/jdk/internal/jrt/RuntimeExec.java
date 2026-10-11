/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Processes"): Runtime.exec for the Go build. jrt's
 * Runtime is hand-written Go, which cannot name the translated ProcessBuilder; a2g gives
 * Runtime's member table the six exec methods (arbace/a2g/out.ae), each a call of the method
 * here of the same parameters. They do what jdk26u's Runtime.exec does (baf63fb,
 * src/java.base/share/classes/java/lang/Runtime.java; Copyright (c) 1995, 2025, Oracle and/or
 * its affiliates, GPL 2 with the Classpath Exception: LICENSE.md), through ProcessBuilder's
 * public API: Runtime's ProcessBuilder.environment(String[]) is package-private, and its
 * effect is the same as clearing the environment the builder copied and putting the variables.
 */
package jdk.internal.jrt;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

/** Runtime.exec's six overloads, for Runtime's member table in the Go build. */
public final class RuntimeExec {

    private RuntimeExec() {
    }

    public static Process exec(String command) throws IOException {
        return exec(command, null, null);
    }

    public static Process exec(String command, String[] envp) throws IOException {
        return exec(command, envp, null);
    }

    public static Process exec(String command, String[] envp, File dir) throws IOException {
        if (command.isEmpty())
            throw new IllegalArgumentException("Empty command");

        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        return exec(cmdarray, envp, dir);
    }

    public static Process exec(String[] cmdarray) throws IOException {
        return exec(cmdarray, null, null);
    }

    public static Process exec(String[] cmdarray, String[] envp) throws IOException {
        return exec(cmdarray, envp, null);
    }

    public static Process exec(String[] cmdarray, String[] envp, File dir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmdarray);
        if (envp != null) {
            // ProcessBuilder.environment(String[]): the variables of envp alone
            Map<String, String> environment = pb.environment();
            environment.clear();
            for (String envstring : envp) {
                // Before 1.5, we blindly passed invalid envstrings
                // to the child process.
                // We would like to throw an exception, but do not,
                // for compatibility with old broken code.

                // Silently discard any trailing junk.
                if (envstring.indexOf((int) '\u0000') != -1)
                    envstring = envstring.replaceFirst("\u0000.*", "");

                int eqlsign = envstring.indexOf('=', 0);
                // Silently ignore envstrings lacking the required `='.
                if (eqlsign != -1)
                    environment.put(envstring.substring(0, eqlsign),
                                    envstring.substring(eqlsign + 1));
            }
        }
        return pb.directory(dir).start();
    }
}
