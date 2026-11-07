/*
 * Copyright (c) 2003, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: F13 (write-ahead)"; JCS161 (a)): jrt's
 * sun.management.VMManagementImpl, jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.management/share/classes/sun/management/VMManagementImpl.java) with its natives, HotSpot's jmm
 * interface in the JDK, written over jrt's (jdk.internal.jrt.HostManagement: the threads' counts, the start,
 * the uptime, the process id) and constants where jrt has nothing to measure yet (classes loaded, compilation,
 * safepoints, GC CPU time: 0, -1 for unsupported; no VM arguments; no performance data, as -XX:-UsePerfData);
 * the optional supports false until their slice but thread contention monitoring (slice 2), the object
 * monitors' and the synchronizers' usage (slice 3b). Copyright (c) Oracle and/or its affiliates, under the GNU
 * General Public License version 2 with the Classpath Exception, as the notice above says (LICENSE.md).
 */

package sun.management;

import jdk.internal.jrt.HostManagement;
import sun.management.counter.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * Implementation of VMManagement interface that accesses the management
 * attributes and operations locally within the same Java virtual
 * machine.
 */
class VMManagementImpl implements VMManagement {

    private static String version;

    private static boolean compTimeMonitoringSupport;
    private static boolean threadContentionMonitoringSupport;
    private static boolean currentThreadCpuTimeSupport;
    private static boolean otherThreadCpuTimeSupport;
    private static boolean objectMonitorUsageSupport;
    private static boolean synchronizerUsageSupport;
    private static boolean threadAllocatedMemorySupport;
    private static boolean gcNotificationSupport;
    private static boolean remoteDiagnosticCommandsSupport;


    static {
        version = getVersion0();
        if (version == null) {
            throw new AssertionError("Invalid Management Version");
        }
        initOptionalSupportFields();
    }
    private static String getVersion0() {
        return "4.0";
    }
    private static void initOptionalSupportFields() {
        // jrt (F13, JCS161 (a)): what jrt measures; the rest unsupported until its slice
        compTimeMonitoringSupport = false;
        threadContentionMonitoringSupport = true;
        currentThreadCpuTimeSupport = false;
        otherThreadCpuTimeSupport = false;
        objectMonitorUsageSupport = true;
        synchronizerUsageSupport = true;
        threadAllocatedMemorySupport = false;
        remoteDiagnosticCommandsSupport = false;
    }

    // Optional supports
    public boolean isCompilationTimeMonitoringSupported() {
        return compTimeMonitoringSupport;
    }

    public boolean isThreadContentionMonitoringSupported() {
        return threadContentionMonitoringSupport;
    }

    public boolean isCurrentThreadCpuTimeSupported() {
        return currentThreadCpuTimeSupport;
    }

    public boolean isOtherThreadCpuTimeSupported() {
        return otherThreadCpuTimeSupport;
    }

    public boolean isBootClassPathSupported() {
        return false;
    }

    public boolean isObjectMonitorUsageSupported() {
        return objectMonitorUsageSupport;
    }

    public boolean isSynchronizerUsageSupported() {
        return synchronizerUsageSupport;
    }

    public boolean isThreadAllocatedMemorySupported() {
        return threadAllocatedMemorySupport;
    }

    public boolean isGcNotificationSupported() {
        boolean isSupported = true;
        try {
            Class.forName("com.sun.management.GarbageCollectorMXBean");
        } catch (ClassNotFoundException x) {
            isSupported = false;
        }
        return isSupported;
    }

    public boolean isRemoteDiagnosticCommandsSupported() {
        return remoteDiagnosticCommandsSupport;
    }

    public boolean isThreadContentionMonitoringEnabled() {
        return HostManagement.contentionMonitoring();
    }
    public boolean isThreadCpuTimeEnabled() {
        return HostManagement.cpuTime();
    }
    public boolean isThreadAllocatedMemoryEnabled() {
        return HostManagement.allocatedMemory();
    }

    // AOT Subsystem
    public boolean endAOTRecording() {
        return false;
    }

    // Class Loading Subsystem
    public int    getLoadedClassCount() {
        long count = getTotalClassCount() - getUnloadedClassCount();
        return (int) count;
    }
    public long getTotalClassCount() {
        return 0;
    }
    public long getUnloadedClassCount() {
        return 0;
    }

    public boolean getVerboseClass() {
        return false;
    }

    // Memory Subsystem
    public long getTotalGcCpuTime() {
        return -1;
    }
    public boolean getVerboseGC() {
        return false;
    }

    // Runtime Subsystem
    public String   getManagementVersion() {
        return version;
    }

    public String getVmId() {
        int pid = getProcessId();
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // ignore
        }

        return pid + "@" + hostname;
    }
    private int getProcessId() {
        return HostManagement.pid0();
    }

    public String   getVmName() {
        return System.getProperty("java.vm.name");
    }

    public String   getVmVendor() {
        return System.getProperty("java.vm.vendor");
    }
    public String   getVmVersion() {
        return System.getProperty("java.vm.version");
    }
    public String   getVmSpecName()  {
        return System.getProperty("java.vm.specification.name");
    }
    public String   getVmSpecVendor() {
        return System.getProperty("java.vm.specification.vendor");
    }
    public String   getVmSpecVersion() {
        return System.getProperty("java.vm.specification.version");
    }
    public String   getClassPath() {
        return System.getProperty("java.class.path");
    }
    public String   getLibraryPath()  {
        return System.getProperty("java.library.path");
    }

    public String   getBootClassPath( ) {
        throw new UnsupportedOperationException(
            "Boot class path mechanism is not supported");
    }

    public long getUptime() {
        return getUptime0();
    }

    private List<String> vmArgs = null;
    public synchronized List<String> getVmArguments() {
        if (vmArgs == null) {
            String[] args = getVmArguments0();
            List<String> l = ((args != null && args.length != 0) ? Arrays.asList(args) :
                                        Collections.<String>emptyList());
            vmArgs = Collections.unmodifiableList(l);
        }
        return vmArgs;
    }
    public String[] getVmArguments0() {
        // no JVM options: Arbace's program has none
        return new String[0];
    }

    public long getStartupTime() {
        return HostManagement.startTime0();
    }
    private long getUptime0() {
        return HostManagement.uptime0();
    }
    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    // Compilation Subsystem
    public String getCompilerName() {
        return System.getProperty("sun.management.compiler");
    }
    public long getTotalCompileTime() {
        return 0;
    }

    // Thread Subsystem
    public long getTotalThreadCount() {
        return HostManagement.totalStarted0();
    }
    public int  getLiveThreadCount() {
        return HostManagement.liveCount0();
    }
    public int  getPeakThreadCount() {
        return HostManagement.peakCount0();
    }
    public int  getDaemonThreadCount() {
        return HostManagement.daemonCount0();
    }

    // Operating System
    public String getOsName() {
        return System.getProperty("os.name");
    }
    public String getOsArch() {
        return System.getProperty("os.arch");
    }
    public String getOsVersion() {
        return System.getProperty("os.version");
    }

    // Hotspot-specific runtime support
    public long getSafepointCount() {
        return 0;
    }
    public long getTotalSafepointTime() {
        return 0;
    }
    public long getSafepointSyncTime() {
        return 0;
    }
    public long getTotalApplicationNonStoppedTime() {
        return 0;
    }

    public long getLoadedClassSize() {
        return 0;
    }
    public long getUnloadedClassSize() {
        return 0;
    }
    public long getClassLoadingTime() {
        return 0;
    }
    public long getMethodDataSize() {
        return 0;
    }
    public long getInitializedClassCount() {
        return 0;
    }
    public long getClassInitializationTime() {
        return 0;
    }
    public long getClassVerificationTime() {
        return 0;
    }

    // Performance Counter Support
    // jrt: no performance data (HotSpot's hsperfdata, jdk.internal.perf.Perf): the JDK's answer with
    // -XX:-UsePerfData
    public List<Counter> getInternalCounters(String pattern) {
        return Collections.emptyList();
    }
}
