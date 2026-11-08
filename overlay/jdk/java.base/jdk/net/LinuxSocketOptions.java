/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS200"): jdk26u's jdk.net.LinuxSocketOptions
 * (https://github.com/openjdk/jdk26u at baf63fb, src/jdk.net/linux/classes/jdk/net/LinuxSocketOptions.java), derived
 * mechanically (test/jrt/derive.ae, each replacement asserted once): its natives (LinuxSocketOptions.c's) given bodies
 * over jdk.internal.jrt.HostChannels, no library loaded; the rest is jdk26u's. The changes Copyright (c) the Arbace
 * authors; Eclipse Public License 1.0. jdk26u's notice follows.
 */
/*
 * Copyright (c) 2017, 2024, Oracle and/or its affiliates. All rights reserved.
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
package jdk.net;

import java.net.SocketException;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.GroupPrincipal;
import jdk.net.ExtendedSocketOptions.PlatformSocketOptions;
import jdk.internal.jrt.HostChannels;
import sun.nio.fs.UnixUserPrincipals;

@SuppressWarnings("restricted")
class LinuxSocketOptions extends PlatformSocketOptions {

    public LinuxSocketOptions() {
    }

    @Override
    void setQuickAck(int fd, boolean on) throws SocketException {
        setQuickAck0(fd, on);
    }

    @Override
    boolean getQuickAck(int fd) throws SocketException {
        return getQuickAck0(fd);
    }

    @Override
    public boolean quickAckSupported() {
        return quickAckSupported0();
    }

    @Override
    boolean keepAliveOptionsSupported() {
        return keepAliveOptionsSupported0();
    }

    @Override
    boolean ipDontFragmentSupported() {
        return true;
    }

    boolean peerCredentialsSupported() {
        return true;
    }

    @Override
    void setTcpKeepAliveProbes(int fd, final int value) throws SocketException {
        setTcpKeepAliveProbes0(fd, value);
    }

    @Override
    void setTcpKeepAliveTime(int fd, final int value) throws SocketException {
        setTcpKeepAliveTime0(fd, value);
    }

    @Override
    void setTcpKeepAliveIntvl(int fd, final int value) throws SocketException {
        setTcpKeepAliveIntvl0(fd, value);
    }

    @Override
    int getTcpKeepAliveProbes(int fd) throws SocketException {
        return getTcpKeepAliveProbes0(fd);
    }

    @Override
    int getTcpKeepAliveTime(int fd) throws SocketException {
        return getTcpKeepAliveTime0(fd);
    }

    @Override
    int getTcpKeepAliveIntvl(int fd) throws SocketException {
        return getTcpKeepAliveIntvl0(fd);
    }

    @Override
    boolean incomingNapiIdSupported() {
        return incomingNapiIdSupported0();
    }

    @Override
    int getIncomingNapiId(int fd) throws SocketException {
        return getIncomingNapiId0(fd);
    }

    @Override
    void setIpDontFragment(int fd, final boolean value, boolean isIPv6) throws SocketException {
        setIpDontFragment0(fd, value, isIPv6);
    }

    @Override
    boolean getIpDontFragment(int fd, boolean isIPv6) throws SocketException {
        return getIpDontFragment0(fd, isIPv6);
    }

    @Override
    UnixDomainPrincipal getSoPeerCred(int fd) throws SocketException {
        long l = getSoPeerCred0(fd);
        int uid = (int)(l >> 32);
        int gid = (int)l;
        UserPrincipal user = UnixUserPrincipals.fromUid(uid);
        GroupPrincipal group = UnixUserPrincipals.fromGid(gid);
        return new UnixDomainPrincipal(user, group);
    }

    // Arbace: LinuxSocketOptions.c's natives over jdk.internal.jrt.HostChannels (go/arbace/jrt/channels_linux.ae): the
    // same calls, levels and names (Linux's, the same on amd64 and arm64), handleError's exceptions
    private static final int SOL_SOCKET = 1, IPPROTO_IP = 0, IPPROTO_TCP = 6, IPPROTO_IPV6 = 41;
    private static final int TCP_KEEPIDLE = 4, TCP_KEEPINTVL = 5, TCP_KEEPCNT = 6, TCP_QUICKACK = 12;
    private static final int IP_MTU_DISCOVER = 10, IPV6_MTU_DISCOVER = 23, IP_PMTUDISC_DONT = 0, IP_PMTUDISC_DO = 2;
    private static final int SO_INCOMING_NAPI_ID = 56;

    private static void setTcpKeepAliveProbes0(int fd, int value) throws SocketException {
        int[] err = new int[1];
        if (HostChannels.extSetInt0(fd, IPPROTO_TCP, TCP_KEEPCNT, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "set option TCP_KEEPCNT failed");
        }
    }

    private static void setTcpKeepAliveTime0(int fd, int value) throws SocketException {
        int[] err = new int[1];
        if (HostChannels.extSetInt0(fd, IPPROTO_TCP, TCP_KEEPIDLE, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "set option TCP_KEEPIDLE failed");
        }
    }

    private static void setTcpKeepAliveIntvl0(int fd, int value) throws SocketException {
        int[] err = new int[1];
        if (HostChannels.extSetInt0(fd, IPPROTO_TCP, TCP_KEEPINTVL, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "set option TCP_KEEPINTVL failed");
        }
    }

    private static void setIpDontFragment0(int fd, boolean value, boolean isIPv6) throws SocketException {
        int optsetting = value ? IP_PMTUDISC_DO : IP_PMTUDISC_DONT;
        int[] err = new int[1];
        if (HostChannels.extSetInt0(fd, isIPv6 ? IPPROTO_IPV6 : IPPROTO_IP, isIPv6 ? IPV6_MTU_DISCOVER : IP_MTU_DISCOVER, optsetting, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "set option IP_DONTFRAGMENT failed");
        }
    }

    private static int getTcpKeepAliveProbes0(int fd) throws SocketException {
        int[] err = new int[1];
        int[] value = new int[1];
        if (HostChannels.extGetInt0(fd, IPPROTO_TCP, TCP_KEEPCNT, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get option TCP_KEEPCNT failed");
        }
        return value[0];
    }

    private static int getTcpKeepAliveTime0(int fd) throws SocketException {
        int[] err = new int[1];
        int[] value = new int[1];
        if (HostChannels.extGetInt0(fd, IPPROTO_TCP, TCP_KEEPIDLE, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get option TCP_KEEPIDLE failed");
        }
        return value[0];
    }

    private static int getTcpKeepAliveIntvl0(int fd) throws SocketException {
        int[] err = new int[1];
        int[] value = new int[1];
        if (HostChannels.extGetInt0(fd, IPPROTO_TCP, TCP_KEEPINTVL, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get option TCP_KEEPINTVL failed");
        }
        return value[0];
    }

    private static boolean getIpDontFragment0(int fd, boolean isIPv6) throws SocketException {
        int[] err = new int[1];
        int[] value = new int[1];
        if (HostChannels.extGetInt0(fd, isIPv6 ? IPPROTO_IPV6 : IPPROTO_IP, isIPv6 ? IPV6_MTU_DISCOVER : IP_MTU_DISCOVER, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get option IP_DONTFRAGMENT failed");
        }
        return value[0] == IP_PMTUDISC_DO;
    }

    private static void setQuickAck0(int fd, boolean on) throws SocketException {
        int[] err = new int[1];
        if (HostChannels.extSetInt0(fd, IPPROTO_TCP, TCP_QUICKACK, on ? 1 : 0, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "set option TCP_QUICKACK failed");
        }
    }

    private static boolean getQuickAck0(int fd) throws SocketException {
        int[] err = new int[1];
        int[] value = new int[1];
        if (HostChannels.extGetInt0(fd, IPPROTO_TCP, TCP_QUICKACK, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get option TCP_QUICKACK failed");
        }
        return value[0] != 0;
    }

    private static long getSoPeerCred0(int fd) throws SocketException {
        int[] err = new int[1];
        int[] ids = new int[2];
        if (HostChannels.extPeerCred0(fd, ids, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get SO_PEERCRED failed");
        }
        return (((long)ids[0]) << 32) | (ids[1] & 0xffffffffL);
    }

    private static boolean keepAliveOptionsSupported0() {
        return HostChannels.extSupported0(IPPROTO_TCP, TCP_KEEPIDLE) && HostChannels.extSupported0(IPPROTO_TCP, TCP_KEEPCNT)
                && HostChannels.extSupported0(IPPROTO_TCP, TCP_KEEPINTVL);
    }

    private static boolean quickAckSupported0() {
        return HostChannels.extSupported0(IPPROTO_TCP, TCP_QUICKACK);
    }

    private static boolean incomingNapiIdSupported0() {
        return HostChannels.extSupported0(SOL_SOCKET, SO_INCOMING_NAPI_ID);
    }

    private static int getIncomingNapiId0(int fd) throws SocketException {
        int[] err = new int[1];
        int[] value = new int[1];
        if (HostChannels.extGetInt0(fd, SOL_SOCKET, SO_INCOMING_NAPI_ID, value, err) == HostChannels.THROWN) {
            RuntimeException u = HostChannels.extUnsupported(err[0]);
            if (u != null)
                throw u;
            throw HostChannels.extException(err[0], "get option SO_INCOMING_NAPI_ID failed");
        }
        return value[0];
    }
}

