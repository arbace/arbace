/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: n-java-f13"): jdk26u's
 * sun.management.MemoryPoolImpl (src/java.management/share/classes/sun/management/MemoryPoolImpl.java
 * at https://github.com/openjdk/jdk26u baf63fb), derived by test/jrt/derive.ae (bin/jrt-convert derive):
 * the natives HotSpot's management interface (jmm) answers have bodies over jrt's sun.management.JrtManagement and jdk.internal.jrt.HostManagement (go/arbace/jrt/memory.ae); the thresholds and the sensors are JrtManagement's low memory detector's (n-java-thresh); nothing else changed. jdk26u's text Copyright (c) Oracle and/or its affiliates, under the GNU General
 * Public License version 2 with the Classpath Exception (LICENSE.md); the derivation Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0.
 */
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

package sun.management;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryType;
import java.lang.management.MemoryManagerMXBean;
import javax.management.ObjectName;

import static java.lang.management.MemoryNotificationInfo.*;

/**
 * Implementation class for a memory pool.
 * Standard and committed hotspot-specific metrics if any.
 *
 * ManagementFactory.getMemoryPoolMXBeans() returns a list of
 * instances of this class.
 */
class MemoryPoolImpl implements MemoryPoolMXBean {

    private final String  name;
    private final boolean isHeap;
    private final boolean isValid;
    private final boolean collectionThresholdSupported;
    private final boolean usageThresholdSupported;

    private MemoryManagerMXBean[] managers;

    private long  usageThreshold;
    private long  collectionThreshold;

    private boolean usageSensorRegistered; // VM-initialized to false
    private boolean gcSensorRegistered;    // VM-initialized to false
    private final Sensor usageSensor;
    private final Sensor gcSensor;

    MemoryPoolImpl(String name, boolean isHeap, long usageThreshold,
                   long gcThreshold) {
        this.name = name;
        this.isHeap = isHeap;
        this.isValid = true;
        this.managers = null;
        this.usageThreshold = usageThreshold;
        this.collectionThreshold = gcThreshold;
        this.usageThresholdSupported = (usageThreshold >= 0);
        this.collectionThresholdSupported = (gcThreshold >= 0);
        this.usageSensor = new PoolSensor(this, name + " usage sensor");
        this.gcSensor = new CollectionSensor(this, name + " collection sensor");
    }

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return isValid;
    }

    public MemoryType getType() {
        if (isHeap) {
            return MemoryType.HEAP;
        } else {
            return MemoryType.NON_HEAP;
        }
    }

    public MemoryUsage getUsage() {
        return getUsage0();
    }

    public synchronized MemoryUsage getPeakUsage() {
        // synchronized since resetPeakUsage may be resetting the peak usage
        return getPeakUsage0();
    }

    public synchronized long getUsageThreshold() {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "Usage threshold is not supported");
        }
        return usageThreshold;
    }

    public void setUsageThreshold(long newThreshold) {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "Usage threshold is not supported");
        }

        MemoryUsage usage = getUsage0();
        if (newThreshold < 0) {
            throw new IllegalArgumentException(
                "Invalid threshold: " + newThreshold);
        }

        if (usage.getMax() != -1 && newThreshold > usage.getMax()) {
            throw new IllegalArgumentException(
                "Invalid threshold: " + newThreshold +
                " must be <= maxSize." +
                " Committed = " + usage.getCommitted() +
                " Max = " + usage.getMax());
        }

        synchronized (this) {
            if (!usageSensorRegistered) {
                // pass the sensor to VM to begin monitoring
                usageSensorRegistered = true;
                setPoolUsageSensor(usageSensor);
            }
            setUsageThreshold0(usageThreshold, newThreshold);
            this.usageThreshold = newThreshold;
        }
    }

    private synchronized MemoryManagerMXBean[] getMemoryManagers() {
        if (managers == null) {
            managers = getMemoryManagers0();
        }
        return managers;
    }

    public String[] getMemoryManagerNames() {
        MemoryManagerMXBean[] mgrs = getMemoryManagers();

        String[] names = new String[mgrs.length];
        for (int i = 0; i < mgrs.length; i++) {
            names[i] = mgrs[i].getName();
        }
        return names;
    }

    public void resetPeakUsage() {
        synchronized (this) {
            // synchronized since getPeakUsage may be called concurrently
            resetPeakUsage0();
        }
    }

    public boolean isUsageThresholdExceeded() {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "Usage threshold is not supported");
        }

        // return false if usage threshold crossing checking is disabled
        if (usageThreshold == 0) {
            return false;
        }

        MemoryUsage u = getUsage0();
        return (u.getUsed() >= usageThreshold ||
                usageSensor.isOn());
    }

    public long getUsageThresholdCount() {
        if (!isUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "Usage threshold is not supported");
        }

        return usageSensor.getCount();
    }

    public boolean isUsageThresholdSupported() {
        return usageThresholdSupported;
    }

    public synchronized long getCollectionUsageThreshold() {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "CollectionUsage threshold is not supported");
        }

        return collectionThreshold;
    }

    public void setCollectionUsageThreshold(long newThreshold) {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "CollectionUsage threshold is not supported");
        }

        MemoryUsage usage = getUsage0();
        if (newThreshold < 0) {
            throw new IllegalArgumentException(
                "Invalid threshold: " + newThreshold);
        }

        if (usage.getMax() != -1 && newThreshold > usage.getMax()) {
            throw new IllegalArgumentException(
                "Invalid threshold: " + newThreshold +
                     " > max (" + usage.getMax() + ").");
        }

        synchronized (this) {
            if (!gcSensorRegistered) {
                // pass the sensor to VM to begin monitoring
                gcSensorRegistered = true;
                setPoolCollectionSensor(gcSensor);
            }
            setCollectionThreshold0(collectionThreshold, newThreshold);
            this.collectionThreshold = newThreshold;
        }
    }

    public boolean isCollectionUsageThresholdExceeded() {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "CollectionUsage threshold is not supported");
        }

        // return false if usage threshold crossing checking is disabled
        if (collectionThreshold == 0) {
            return false;
        }

        MemoryUsage u = getCollectionUsage0();
        return (gcSensor.isOn() ||
                (u != null && u.getUsed() >= collectionThreshold));
    }

    public long getCollectionUsageThresholdCount() {
        if (!isCollectionUsageThresholdSupported()) {
            throw new UnsupportedOperationException(
                "CollectionUsage threshold is not supported");
        }

        return gcSensor.getCount();
    }

    public MemoryUsage getCollectionUsage() {
        return getCollectionUsage0();
    }

    public boolean isCollectionUsageThresholdSupported() {
        return collectionThresholdSupported;
    }

    // Native VM support
    private MemoryUsage getUsage0() {
        return JrtManagement.usage(isHeap);
    }
    private MemoryUsage getPeakUsage0() {
        return JrtManagement.peak(isHeap);
    }
    private MemoryUsage getCollectionUsage0() {
        return JrtManagement.collectionUsage(isHeap);
    }
    private void setUsageThreshold0(long current, long newThreshold) {
        JrtManagement.setThreshold(this, JrtManagement.USAGE, newThreshold);
    }
    private void setCollectionThreshold0(long current, long newThreshold) {
        JrtManagement.setThreshold(this, JrtManagement.COLLECTION, newThreshold);
    }
    private void resetPeakUsage0() {
        JrtManagement.resetPeak(isHeap);
    }
    private MemoryManagerMXBean[] getMemoryManagers0() {
        return JrtManagement.managersOf(this);
    }
    private void setPoolUsageSensor(Sensor s) {
        JrtManagement.setSensor(this, JrtManagement.USAGE, s);
    }
    private void setPoolCollectionSensor(Sensor s) {
        JrtManagement.setSensor(this, JrtManagement.COLLECTION, s);
    }

    // package private

    /**
     * PoolSensor will be triggered by the VM when the memory
     * usage of a memory pool is crossing the usage threshold.
     * The VM will not trigger this sensor in subsequent crossing
     * unless the memory usage has returned below the threshold.
     */
    static class PoolSensor extends Sensor {
        final MemoryPoolImpl pool;

        PoolSensor(MemoryPoolImpl pool, String name) {
            super(name);
            this.pool = pool;
        }
        void triggerAction(MemoryUsage usage) {
            // create and send notification
            MemoryImpl.createNotification(MEMORY_THRESHOLD_EXCEEDED,
                                          pool.getName(),
                                          usage,
                                          getCount());
        }
        void triggerAction() {
            // do nothing
        }
        void clearAction() {
            // do nothing
        }
    }

    /**
     * CollectionSensor will be triggered and cleared by the VM
     * when the memory usage of a memory pool after GC is crossing
     * the collection threshold.
     * The VM will trigger this sensor in subsequent crossing
     * regardless if the memory usage has changed since the previous GC.
     */
    class CollectionSensor extends Sensor {
        final MemoryPoolImpl pool;
        CollectionSensor(MemoryPoolImpl pool, String name) {
            super(name);
            this.pool = pool;
        }
        void triggerAction(MemoryUsage usage) {
            MemoryImpl.createNotification(MEMORY_COLLECTION_THRESHOLD_EXCEEDED,
                                          pool.getName(),
                                          usage,
                                          gcSensor.getCount());
        }
        void triggerAction() {
            // do nothing
        }
        void clearAction() {
            // do nothing
        }
    }

    public ObjectName getObjectName() {
        return Util.newObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE, getName());
    }

}
