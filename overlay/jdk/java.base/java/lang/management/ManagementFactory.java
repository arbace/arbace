/*
 * Copyright (c) 2003, 2024, Oracle and/or its affiliates. All rights reserved.
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
 * java.lang.management.ManagementFactory, jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.management/share/classes/java/lang/management/ManagementFactory.java) without JMX (JCS95 (a)): no
 * platform MBean server (getPlatformMBeanServer), no MXBean proxies over an MBeanServerConnection
 * (newPlatformMXBeanProxy and the getPlatformMXBean(s) taking one), no PlatformMBeanProvider found by
 * ServiceLoader. The platform components are a table of jrt's own, in jdk26u's precedence (jdk.management's
 * provider before java.management's): the threads' bean (jdk.management's HotSpotThreadImpl, which is also a
 * com.sun.management.ThreadMXBean) and the runtime's (sun.management.RuntimeImpl); the other interfaces of the
 * platform (memory, class loading, compilation, the collectors, the pools and managers, the operating system,
 * logging, buffer pools) are F13's later slices: asking for one throws UnsupportedOperationException, naming the
 * slice. The constants and the getters' contracts are jdk26u's. Copyright (c) Oracle and/or its affiliates, under
 * the GNU General Public License version 2 with the Classpath Exception, as the notice above says (LICENSE.md).
 */
package java.lang.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManagementFactory {
    // A class with only static fields and methods.
    private ManagementFactory() {};

    public static final String CLASS_LOADING_MXBEAN_NAME =
        "java.lang:type=ClassLoading";

    public static final String COMPILATION_MXBEAN_NAME =
        "java.lang:type=Compilation";

    public static final String MEMORY_MXBEAN_NAME =
        "java.lang:type=Memory";

    public static final String OPERATING_SYSTEM_MXBEAN_NAME =
        "java.lang:type=OperatingSystem";

    public static final String RUNTIME_MXBEAN_NAME =
        "java.lang:type=Runtime";

    public static final String THREAD_MXBEAN_NAME =
        "java.lang:type=Threading";

    public static final String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE =
        "java.lang:type=GarbageCollector";

    public static final String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE=
        "java.lang:type=MemoryManager";

    public static final String MEMORY_POOL_MXBEAN_DOMAIN_TYPE=
        "java.lang:type=MemoryPool";

    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return getPlatformMXBean(ClassLoadingMXBean.class);
    }

    public static MemoryMXBean getMemoryMXBean() {
        return getPlatformMXBean(MemoryMXBean.class);
    }

    public static ThreadMXBean getThreadMXBean() {
        return getPlatformMXBean(ThreadMXBean.class);
    }

    public static RuntimeMXBean getRuntimeMXBean() {
        return getPlatformMXBean(RuntimeMXBean.class);
    }

    public static CompilationMXBean getCompilationMXBean() {
        return getPlatformMXBean(CompilationMXBean.class);
    }

    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return getPlatformMXBean(OperatingSystemMXBean.class);
    }

    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return getPlatformMXBeans(MemoryPoolMXBean.class);
    }

    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return getPlatformMXBeans(MemoryManagerMXBean.class);
    }

    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return getPlatformMXBeans(GarbageCollectorMXBean.class);
    }

    // jrt: the platform's management interfaces (jdk26u's DefaultPlatformMBeanProvider's and
    // jdk.management's PlatformMBeanProviderImpl's), and the ones whose bean the Go build makes
    private static final String[] PLATFORM_INTERFACES = {
        "java.lang.management.ClassLoadingMXBean", "java.lang.management.CompilationMXBean",
        "java.lang.management.MemoryMXBean", "java.lang.management.GarbageCollectorMXBean",
        "java.lang.management.MemoryManagerMXBean", "java.lang.management.MemoryPoolMXBean",
        "java.lang.management.OperatingSystemMXBean", "java.lang.management.PlatformLoggingMXBean",
        "java.lang.management.RuntimeMXBean", "java.lang.management.ThreadMXBean",
        "java.lang.management.BufferPoolMXBean", "com.sun.management.GarbageCollectorMXBean",
        "com.sun.management.OperatingSystemMXBean", "com.sun.management.UnixOperatingSystemMXBean",
        "com.sun.management.ThreadMXBean", "com.sun.management.HotSpotDiagnosticMXBean"
    };

    private static boolean isPlatformInterface(Class<?> c) {
        String n = c.getName();
        for (String p : PLATFORM_INTERFACES) {
            if (p.equals(n)) {
                return true;
            }
        }
        return false;
    }

    /** The singleton bean of a platform interface the Go build makes, else null. */
    private static PlatformManagedObject singleton(Class<?> intf) {
        String n = intf.getName();
        if (n.equals("java.lang.management.ThreadMXBean") || n.equals("com.sun.management.ThreadMXBean")) {
            return threadBean();
        }
        if (n.equals("java.lang.management.RuntimeMXBean")) {
            return sun.management.ManagementFactoryHelper.getRuntimeMXBean();
        }
        return null;
    }

    private static com.sun.management.internal.HotSpotThreadImpl threadImpl;

    private static synchronized PlatformManagedObject threadBean() {
        if (threadImpl == null) {
            // jrt: the threads' held monitors and synchronizers recorded from here (F13 slice 3b)
            jdk.internal.jrt.HostManagement.trackLocks0();
            threadImpl = new com.sun.management.internal.HotSpotThreadImpl(
                    sun.management.ManagementFactoryHelper.getVMManagement());
        }
        return threadImpl;
    }

    private static UnsupportedOperationException notYet(Class<?> intf) {
        return new UnsupportedOperationException(intf.getName()
            + " is not in the Go build yet (doc/classes/JAVA-DEFERRED-TRIAGE.md, F13's later slices)");
    }

    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(Class<T> mxbeanInterface) {
        if (!isPlatformInterface(mxbeanInterface)) {
            throw new IllegalArgumentException(mxbeanInterface.getName() +
                    " is not a platform management interface");
        }
        PlatformManagedObject o = singleton(mxbeanInterface);
        if (o == null) {
            throw notYet(mxbeanInterface);
        }
        return mxbeanInterface.cast(o);
    }

    public static <T extends PlatformManagedObject> List<T>
            getPlatformMXBeans(Class<T> mxbeanInterface) {
        if (!isPlatformInterface(mxbeanInterface)) {
            throw new IllegalArgumentException(mxbeanInterface.getName()
                    + " is not a platform management interface");
        }
        PlatformManagedObject o = singleton(mxbeanInterface);
        if (o == null) {
            throw notYet(mxbeanInterface);
        }
        List<T> l = new ArrayList<>();
        l.add(mxbeanInterface.cast(o));
        return l;
    }

    public static Set<Class<? extends PlatformManagedObject>>
           getPlatformManagementInterfaces()
    {
        Set<Class<? extends PlatformManagedObject>> s = new HashSet<>();
        s.add(ThreadMXBean.class);
        s.add(com.sun.management.ThreadMXBean.class);
        s.add(RuntimeMXBean.class);
        return Collections.unmodifiableSet(s);
    }
}
