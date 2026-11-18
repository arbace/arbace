/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13 slice 5, "As built: F13 (write-ahead)"; JCS161 (a)):
 * jrt's com.sun.management.internal.OperatingSystemImpl, jdk26u's (https://github.com/openjdk/jdk26u at baf63fb,
 * src/jdk.management/unix/classes/com/sun/management/internal/OperatingSystemImpl.java) with its natives,
 * libmanagement_ext's in the JDK, written over jrt's (jdk.internal.jrt.HostManagement: go/arbace/jrt's
 * management_linux.ae reads what the JDK's C reads on Linux), no cgroup metrics (jdk.internal.platform's
 * Container is not in the Go build: the host's facts, as the JDK's without container limits, where
 * Container.metrics() is null; since n-java-mx, 2026-11-18, the container branches and their ContainerCpuTicks cut
 * rather than kept behind a null field, since j2g refuses every method that names Metrics: getTotalMemorySize,
 * getFreeMemorySize, the swap sizes and the CPU loads threw "jdk.internal.platform.Metrics is not in the closed
 * world"); jrt's JrtPlatformBeans makes it for ManagementFactory's table (jdk.management's provider is not in
 * the Go build). Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2 with the
 * Classpath Exception, as the notice above says (LICENSE.md).
 */

package com.sun.management.internal;

import jdk.internal.jrt.HostManagement;
import sun.management.BaseOperatingSystemImpl;
import sun.management.VMManagement;
/**
 * Implementation class for the operating system.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getOperatingSystemMXBean() returns an instance
 * of this class.
 */
class OperatingSystemImpl extends BaseOperatingSystemImpl
    implements com.sun.management.UnixOperatingSystemMXBean {

    OperatingSystemImpl(VMManagement vm) {
        super(vm);
        // jrt: no cgroup metrics (jdk.internal.platform is not in the Go build): the host's facts, as the JDK's
        // without container limits (containerMetrics null)
    }

    public long getCommittedVirtualMemorySize() {
        return getCommittedVirtualMemorySize0();
    }

    public long getTotalSwapSpaceSize() {
        return getTotalSwapSpaceSize0();
    }

    public long getFreeSwapSpaceSize() {
        return getFreeSwapSpaceSize0();
    }

    public long getProcessCpuTime() {
        return getProcessCpuTime0();
    }

    public long getFreeMemorySize() {
        return getFreeMemorySize0();
    }

    public long getTotalMemorySize() {
        return getTotalMemorySize0();
    }

    public long getOpenFileDescriptorCount() {
        return getOpenFileDescriptorCount0();
    }

    public long getMaxFileDescriptorCount() {
        return getMaxFileDescriptorCount0();
    }

    public double getCpuLoad() {
        return getCpuLoad0();
    }

    public double getProcessCpuLoad() {
        return getProcessCpuLoad0();
    }

    /* native methods */
    private long getCommittedVirtualMemorySize0() {
        return HostManagement.os0(0);
    }
    private long getFreeMemorySize0() {
        return HostManagement.os0(1);
    }
    private long getFreeSwapSpaceSize0() {
        return HostManagement.os0(3);
    }
    private long getMaxFileDescriptorCount0() {
        return HostManagement.os0(7);
    }
    private long getOpenFileDescriptorCount0() {
        return HostManagement.os0(6);
    }
    private long getProcessCpuTime0() {
        return HostManagement.os0(5);
    }
    private double getProcessCpuLoad0() {
        return HostManagement.processCpuLoad0();
    }
    private double getCpuLoad0() {
        return HostManagement.cpuLoad0(-1);
    }
    private long getTotalMemorySize0() {
        return HostManagement.os0(2);
    }
    private long getTotalSwapSpaceSize0() {
        return HostManagement.os0(4);
    }
    private double getSingleCpuLoad0(int cpuNum) {
        return HostManagement.cpuLoad0(cpuNum);
    }
    private int getHostConfiguredCpuCount0() {
        return (int) HostManagement.os0(8);
    }
    private int getHostOnlineCpuCount0() {
        return (int) HostManagement.os0(9);
    }
    // CPU ticks since boot in nanoseconds
    private long getHostTotalCpuTicks0() {
        return HostManagement.os0(10);
    }

    static {
        initialize0();
    }

    private static void initialize0() {
    }
}
