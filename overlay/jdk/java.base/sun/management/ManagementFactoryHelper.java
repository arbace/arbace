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
 * sun.management.ManagementFactoryHelper, the part of jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.management/share/classes/sun/management/ManagementFactoryHelper.java) that makes the platform beans,
 * transcribed: the VM's management object and the beans made once. Its MBean registration, the logging and
 * buffer-pool beans' JMX adapters and the HotSpot internal MBeans (javax.management's MBean server, by design:
 * JCS95 (a)) are not in the Go build; since n-java-f13 the class loading and memory beans and the pools, managers
 * and collectors (jdk26u's getters, the pools and managers made by jrt's sun.management.JrtManagement where jdk26u's
 * VM makes them); the compilation bean is none (no JIT compiler: ManagementFactory's header). toThreadState maps
 * jmm's thread status as jdk26u's jdk.internal.misc.VM.toThreadState does (jrt's VM is a shim of its own). Copyright (c) Oracle and/or
 * its affiliates, under the GNU General Public License version 2 with the Classpath Exception, as the notice
 * above says (LICENSE.md).
 *
 * JCS281 (b) (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: P2 (by day)"): jdk26u's PlatformLoggingImpl, the
 * platform logging bean, over java.util.logging's LoggingMXBean called directly (java.logging is in the world,
 * translated as java.base's: jdk26u's LoggingMXBeanAccess finds it by reflection only because java.management
 * may not read java.logging's module).
 */
package sun.management;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.PlatformLoggingMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public class ManagementFactoryHelper {

    private static final VMManagement jvm = new VMManagementImpl();

    private ManagementFactoryHelper() {};

    public static VMManagement getVMManagement() {
        return jvm;
    }

    private static ClassLoadingImpl    classMBean = null;
    private static MemoryImpl          memoryMBean = null;
    private static ThreadImpl          threadMBean = null;
    private static RuntimeImpl         runtimeMBean = null;

    // n-java-f13: jdk26u's
    public static synchronized ClassLoadingMXBean getClassLoadingMXBean() {
        if (classMBean == null) {
            classMBean = new ClassLoadingImpl(jvm);
        }
        return classMBean;
    }

    public static synchronized MemoryMXBean getMemoryMXBean() {
        if (memoryMBean == null) {
            memoryMBean = new MemoryImpl(jvm);
        }
        return memoryMBean;
    }

    public static synchronized ThreadMXBean getThreadMXBean() {
        if (threadMBean == null) {
            jdk.internal.jrt.HostManagement.trackLocks0();
            threadMBean = new ThreadImpl(jvm);
        }
        return threadMBean;
    }

    public static synchronized RuntimeMXBean getRuntimeMXBean() {
        if (runtimeMBean == null) {
            runtimeMBean = new RuntimeImpl(jvm);
        }
        return runtimeMBean;
    }

    // n-java-f13: jdk26u's
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        MemoryPoolMXBean[] pools = MemoryImpl.getMemoryPools();
        List<MemoryPoolMXBean> list = new ArrayList<>(pools.length);
        for (MemoryPoolMXBean p : pools) {
            list.add(p);
        }
        return list;
    }

    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        MemoryManagerMXBean[]  mgrs = MemoryImpl.getMemoryManagers();
        List<MemoryManagerMXBean> result = new ArrayList<>(mgrs.length);
        for (MemoryManagerMXBean m : mgrs) {
            result.add(m);
        }
        return result;
    }

    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        MemoryManagerMXBean[]  mgrs = MemoryImpl.getMemoryManagers();
        List<GarbageCollectorMXBean> result = new ArrayList<>(mgrs.length);
        for (MemoryManagerMXBean m : mgrs) {
            if (GarbageCollectorMXBean.class.isInstance(m)) {
                 result.add(GarbageCollectorMXBean.class.cast(m));
            }
        }
        return result;
    }

    /**
     * Returns an array of the name of all memory pools.  The order of the memory pools is
     * significant and maintained in the VM. (n-java-f13: jdk26u's stream as a loop)
     */
    public static String[] getAllMemoryPoolNames() {
        MemoryPoolMXBean[] pools = MemoryImpl.getMemoryPools();
        String[] names = new String[pools.length];
        for (int i = 0; i < pools.length; i++) {
            names[i] = pools[i].getName();
        }
        return names;
    }

    // JCS281 (b): jdk26u's getPlatformLoggingMXBean, java.util.logging always present
    public static PlatformLoggingMXBean getPlatformLoggingMXBean() {
        return PlatformLoggingImpl.MBEAN;
    }

    public static boolean isPlatformLoggingMXBeanAvailable() {
        return true;
    }

    // jdk26u's, its LoggingMXBeanAccess's reflective calls made directly on LogManager's bean
    static final class PlatformLoggingImpl implements PlatformLoggingMXBean {

        @SuppressWarnings("deprecation")
        private final java.util.logging.LoggingMXBean impl = java.util.logging.LogManager.getLoggingMXBean();

        private PlatformLoggingImpl() {
        }

        private volatile javax.management.ObjectName objname;  // created lazily
        @Override
        public javax.management.ObjectName getObjectName() {
            javax.management.ObjectName result = objname;
            if (result == null) {
                synchronized (this) {
                    result = objname;
                    if (result == null) {
                        result = Util.newObjectName(LOGGING_MXBEAN_NAME);
                        objname = result;
                    }
                }
            }
            return result;
        }

        @Override
        public java.util.List<String> getLoggerNames() {
            return impl.getLoggerNames();
        }

        @Override
        public String getLoggerLevel(String loggerName) {
            return impl.getLoggerLevel(loggerName);
        }

        @Override
        public void setLoggerLevel(String loggerName, String levelName) {
            impl.setLoggerLevel(loggerName, levelName);
        }

        @Override
        public String getParentLoggerName(String loggerName) {
            return impl.getParentLoggerName(loggerName);
        }

        static final PlatformLoggingMXBean MBEAN = new PlatformLoggingImpl();
    }

    private static final String LOGGING_MXBEAN_NAME = "java.util.logging:type=Logging";

    public static boolean isThreadSuspended(int state) {
        return ((state & JMM_THREAD_STATE_FLAG_SUSPENDED) != 0);
    }

    public static boolean isThreadRunningNative(int state) {
        return ((state & JMM_THREAD_STATE_FLAG_NATIVE) != 0);
    }

    public static Thread.State toThreadState(int state) {
        // suspended and native bits may be set in state
        int threadStatus = state & ~JMM_THREAD_STATE_FLAG_MASK;
        // jdk26u's jdk.internal.misc.VM.toThreadState
        if ((threadStatus & JVMTI_THREAD_STATE_RUNNABLE) != 0) {
            return Thread.State.RUNNABLE;
        } else if ((threadStatus & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) != 0) {
            return Thread.State.BLOCKED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_INDEFINITELY) != 0) {
            return Thread.State.WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT) != 0) {
            return Thread.State.TIMED_WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_TERMINATED) != 0) {
            return Thread.State.TERMINATED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_ALIVE) == 0) {
            return Thread.State.NEW;
        } else {
            return Thread.State.RUNNABLE;
        }
    }

    // These values are defined in jmm.h
    private static final int JMM_THREAD_STATE_FLAG_MASK = 0xFFF00000;
    private static final int JMM_THREAD_STATE_FLAG_SUSPENDED = 0x00100000;
    private static final int JMM_THREAD_STATE_FLAG_NATIVE = 0x00400000;

    // jdk.internal.misc.VM's, from jvmti.h
    private static final int JVMTI_THREAD_STATE_ALIVE = 0x0001;
    private static final int JVMTI_THREAD_STATE_TERMINATED = 0x0002;
    private static final int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;
    private static final int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;
    private static final int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;
    private static final int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;
}
