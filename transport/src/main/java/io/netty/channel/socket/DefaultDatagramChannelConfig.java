/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.socket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.internal.DetectionUtil;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;

import static io.netty.channel.ChannelOption.*;

/**
 * The default {@link DatagramChannelConfig} implementation.
 */
public class DefaultDatagramChannelConfig extends DefaultChannelConfig implements DatagramChannelConfig {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultDatagramChannelConfig.class);

    private static final int DEFAULT_RECEIVE_PACKET_SIZE = 2048;

    private final DatagramSocket javaSocket;
    private volatile int receivePacketSize = DEFAULT_RECEIVE_PACKET_SIZE;

    /**
     * Creates a new instance.
     */
    public DefaultDatagramChannelConfig(DatagramChannel channel, DatagramSocket javaSocket) {
        super(channel);
        if (javaSocket == null) {
            throw new NullPointerException("javaSocket");
        }
        this.javaSocket = javaSocket;
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(
                super.getOptions(),
                SO_BROADCAST, SO_RCVBUF, SO_SNDBUF, SO_REUSEADDR, IP_MULTICAST_LOOP_DISABLED,
                IP_MULTICAST_ADDR, IP_MULTICAST_IF, IP_MULTICAST_TTL, IP_TOS, UDP_RECEIVE_PACKET_SIZE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        if (option == SO_BROADCAST) {
            return (T) Boolean.valueOf(isBroadcast());
        }
        if (option == SO_RCVBUF) {
            return (T) Integer.valueOf(getReceiveBufferSize());
        }
        if (option == SO_SNDBUF) {
            return (T) Integer.valueOf(getSendBufferSize());
        }
        if (option == UDP_RECEIVE_PACKET_SIZE) {
            return (T) Integer.valueOf(getReceivePacketSize());
        }
        if (option == SO_REUSEADDR) {
            return (T) Boolean.valueOf(isReuseAddress());
        }
        if (option == IP_MULTICAST_LOOP_DISABLED) {
            return (T) Boolean.valueOf(isLoopbackModeDisabled());
        }
        if (option == IP_MULTICAST_ADDR) {
            T i = (T) getInterface();
            return i;
        }
        if (option == IP_MULTICAST_IF) {
            T i = (T) getNetworkInterface();
            return i;
        }
        if (option == IP_MULTICAST_TTL) {
            return (T) Integer.valueOf(getTimeToLive());
        }
        if (option == IP_TOS) {
            return (T) Integer.valueOf(getTrafficClass());
        }

        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);

        if (option == SO_BROADCAST) {
            setBroadcast((Boolean) value);
        } else if (option == SO_RCVBUF) {
            setReceiveBufferSize((Integer) value);
        } else if (option == SO_SNDBUF) {
            setSendBufferSize((Integer) value);
        } else if (option == SO_REUSEADDR) {
            setReuseAddress((Boolean) value);
        } else if (option == IP_MULTICAST_LOOP_DISABLED) {
            setLoopbackModeDisabled((Boolean) value);
        } else if (option == IP_MULTICAST_ADDR) {
            setInterface((InetAddress) value);
        } else if (option == IP_MULTICAST_IF) {
            setNetworkInterface((NetworkInterface) value);
        } else if (option == IP_MULTICAST_TTL) {
            setTimeToLive((Integer) value);
        } else if (option == IP_TOS) {
            setTrafficClass((Integer) value);
        } else {
            return super.setOption(option, value);
        }

        return true;
    }

    @Override
    public boolean isBroadcast() {
        try {
            return javaSocket.getBroadcast();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public DatagramChannelConfig setBroadcast(boolean broadcast) {
        try {
            // See: https://github.com/netty/netty/issues/576
            if (broadcast &&
                !DetectionUtil.isWindows() && !DetectionUtil.isRoot() &&
                !javaSocket.getLocalAddress().isAnyLocalAddress()) {
                // Warn a user about the fact that a non-root user can't receive a
                // broadcast packet on *nix if the socket is bound on non-wildcard address.
                logger.warn(
                        "A non-root user can't receive a broadcast packet if the socket " +
                        "is not bound to a wildcard address; setting the SO_BROADCAST flag " +
                        "anyway as requested on the socket which is bound to " +
                        javaSocket.getLocalSocketAddress() + '.');
            }

            javaSocket.setBroadcast(broadcast);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public InetAddress getInterface() {
        if (javaSocket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket) javaSocket).getInterface();
            } catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public DatagramChannelConfig setInterface(InetAddress interfaceAddress) {
        if (javaSocket instanceof MulticastSocket) {
            try {
                ((MulticastSocket) javaSocket).setInterface(interfaceAddress);
            } catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return this;
    }

    @Override
    public boolean isLoopbackModeDisabled() {
        if (javaSocket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket) javaSocket).getLoopbackMode();
            } catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public DatagramChannelConfig setLoopbackModeDisabled(boolean loopbackModeDisabled) {
        if (javaSocket instanceof MulticastSocket) {
            try {
                ((MulticastSocket) javaSocket).setLoopbackMode(loopbackModeDisabled);
            } catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return this;
    }

    @Override
    public NetworkInterface getNetworkInterface() {
        if (javaSocket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket) javaSocket).getNetworkInterface();
            } catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public DatagramChannelConfig setNetworkInterface(NetworkInterface networkInterface) {
        if (javaSocket instanceof MulticastSocket) {
            try {
                ((MulticastSocket) javaSocket).setNetworkInterface(networkInterface);
            } catch (SocketException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return this;
    }

    @Override
    public boolean isReuseAddress() {
        try {
            return javaSocket.getReuseAddress();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public DatagramChannelConfig setReuseAddress(boolean reuseAddress) {
        try {
            javaSocket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public int getReceiveBufferSize() {
        try {
            return javaSocket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public DatagramChannelConfig setReceiveBufferSize(int receiveBufferSize) {
        try {
            javaSocket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public int getSendBufferSize() {
        try {
            return javaSocket.getSendBufferSize();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public DatagramChannelConfig setSendBufferSize(int sendBufferSize) {
        try {
            javaSocket.setSendBufferSize(sendBufferSize);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public int getReceivePacketSize() {
        return receivePacketSize;
    }

    @Override
    public DatagramChannelConfig setReceivePacketSize(int receivePacketSize) {
        if (receivePacketSize <= 0) {
            throw new IllegalArgumentException(
                    String.format("receivePacketSize: %d (expected: > 0)", receivePacketSize));
        }
        this.receivePacketSize = receivePacketSize;
        return this;
    }

    @Override
    public int getTimeToLive() {
        if (javaSocket instanceof MulticastSocket) {
            try {
                return ((MulticastSocket) javaSocket).getTimeToLive();
            } catch (IOException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public DatagramChannelConfig setTimeToLive(int ttl) {
        if (javaSocket instanceof MulticastSocket) {
            try {
                ((MulticastSocket) javaSocket).setTimeToLive(ttl);
            } catch (IOException e) {
                throw new ChannelException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
        return this;
    }

    @Override
    public int getTrafficClass() {
        try {
            return javaSocket.getTrafficClass();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public DatagramChannelConfig setTrafficClass(int trafficClass) {
        try {
            javaSocket.setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public DatagramChannelConfig setWriteSpinCount(int writeSpinCount) {
        return (DatagramChannelConfig) super.setWriteSpinCount(writeSpinCount);
    }

    @Override
    public DatagramChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        return (DatagramChannelConfig) super.setConnectTimeoutMillis(connectTimeoutMillis);
    }

    @Override
    public DatagramChannelConfig setAllocator(ByteBufAllocator allocator) {
        return (DatagramChannelConfig) super.setAllocator(allocator);
    }

    @Override
    public DatagramChannelConfig setAutoRead(boolean autoRead) {
        return (DatagramChannelConfig) super.setAutoRead(autoRead);
    }
}