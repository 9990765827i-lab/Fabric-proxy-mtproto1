package com.github.mtprotoproxy.proxy;

import com.github.mtprotoproxy.ConfigManager;
import com.github.mtprotoproxy.MTProtoProxyMod;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

import java.net.InetSocketAddress;

public class MTProtoProxyInitializer extends ChannelInitializer<SocketChannel> {
    private static final EventLoopGroup workerGroup;
    private static final Class<? extends SocketChannel> socketChannelClass;

    static {
        if (Epoll.isAvailable()) {
            workerGroup = new EpollEventLoopGroup();
            socketChannelClass = EpollSocketChannel.class;
        } else {
            workerGroup = new NioEventLoopGroup();
            socketChannelClass = NioSocketChannel.class;
        }
    }

    private final InetSocketAddress targetAddress;

    public MTProtoProxyInitializer(InetSocketAddress targetAddress) {
        this.targetAddress = targetAddress;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        // Add MTProto proxy handler
        pipeline.addLast(new MTProtoProxyHandler(targetAddress));
    }

    public static EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public static Class<? extends SocketChannel> getSocketChannelClass() {
        return socketChannelClass;
    }
}