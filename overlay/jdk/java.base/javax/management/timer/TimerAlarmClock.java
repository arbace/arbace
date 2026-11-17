/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JMX-1 (write-ahead)"; JCS263): jdk26u's
 * javax.management.timer.TimerAlarmClock (src/java.management/share/classes/javax/management/timer/TimerAlarmClock.java
 * at https://github.com/openjdk/jdk26u baf63fb), derived by test/jrt/derive.ae (bin/jrt-convert derive):
 * System.Logger, which jrt's hand-written java.lang.System has not (JCS155), is jdk.internal.jrt.SystemLoggers.Logger as F24 made it for jdk.httpserver (the same interface over java.util.logging, its Level the same constants); nothing else changed. jdk26u's text Copyright (c) Oracle and/or its affiliates, under the GNU General
 * Public License version 2 with the Classpath Exception (LICENSE.md); the derivation Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0.
 */
/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.timer;

import java.util.Date;
import jdk.internal.jrt.SystemLoggers.Logger.Level;
import static com.sun.jmx.defaults.JmxProperties.TIMER_LOGGER;

/**
 * This class provides a simple implementation of an alarm clock MBean.
 * The aim of this MBean is to set up an alarm which wakes up the timer every timeout (fixed-delay)
 * or at the specified date (fixed-rate).
 */

class TimerAlarmClock extends java.util.TimerTask {

    Timer listener = null;
    long timeout = 10000;
    Date next = null;

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    public TimerAlarmClock(Timer listener, long timeout) {
        this.listener = listener;
        this.timeout = Math.max(0L, timeout);
    }

    public TimerAlarmClock(Timer listener, Date next) {
        this.listener = listener;
        this.next = next;
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * This method is called by the timer when it is started.
     */
    public void run() {

        try {
            //this.sleep(timeout);
            TimerAlarmClockNotification notif = new TimerAlarmClockNotification(this);
            listener.notifyAlarmClock(notif);
        } catch (Exception e) {
            TIMER_LOGGER.log(Level.TRACE,
                    "Got unexpected exception when sending a notification", e);
        }
    }
}
