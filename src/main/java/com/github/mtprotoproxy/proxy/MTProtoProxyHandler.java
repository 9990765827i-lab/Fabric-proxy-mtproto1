package com.github.mtprotoproxy.proxy;

import com.github.mtprotoproxy.ConfigManager;
import com.github.mtprotoproxy.MTProtoProxyMod;
import com.github.mtprotoproxy.crypto.AES256IGE;
import com.github.mtprotoproxy.crypto.Obfuscated2;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.thread.ThreadExecutor;

import java.net.InetSocketAddress;

public class MTProtoProxyHandler extends ChannelInboundHandlerAdapter {
    private final InetSocketAddress targetAddress;
    private Channel outboundChannel;
    private ClientConnection clientConnection;
    private boolean handshakeDone = false;
    private AES256IGE encryptor;
    private AES256IGE decryptor;

    public MTProtoProxyHandler(InetSocketAddress targetAddress) {
        this.targetAddress = targetAddress;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        // Create a fake ClientConnection to integrate with Minecraft network system
        clientConnection = ClientConnection.create(NetworkSide.CLIENT);
        // We will manually push packets to it
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Connect to target Minecraft server through proxy
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new BackendHandler());
                    }
                });
        ChannelFuture future = bootstrap.connect(targetAddress);
        outboundChannel = future.channel();
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                MTProtoProxyMod.LOGGER.info("Connected to target server {}", targetAddress);
                performMTProtoHandshake(ctx);
            } else {
                MTProtoProxyMod.LOGGER.error("Failed to connect to target server", f.cause());
                ctx.close();
            }
        });
    }

    private void performMTProtoHandshake(ChannelHandlerContext ctx) {
        byte[] secret = ConfigManager.getSecret();
        if (secret == null) {
            MTProtoProxyMod.LOGGER.error("No secret configured, cannot perform MTProto handshake");
            ctx.close();
            return;
        }

        try {
            Obfuscated2 obf = new Obfuscated2(secret);
            byte[] handshakeData = obf.generateHandshake();
            ByteBuf handshakeBuf = Unpooled.wrappedBuffer(handshakeData);
            ctx.writeAndFlush(handshakeBuf).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    MTProtoProxyMod.LOGGER.info("MTProto handshake sent, waiting for response");
                    // The server should respond with its own handshake (or encrypted data)
                    // For simplicity, after sending we consider handshake done and use AES from secret
                    byte[] key = obf.getAesKey();
                    byte[] iv = obf.getAesIv();
                    encryptor = new AES256IGE(key, iv);
                    decryptor = new AES256IGE(key, iv);
                    handshakeDone = true;
                    // Now we can forward traffic
                    // Trigger reading from backend
                    if (outboundChannel.isActive()) {
                        outboundChannel.config().setAutoRead(true);
                    }
                } else {
                    MTProtoProxyMod.LOGGER.error("Failed to send MTProto handshake", f.cause());
                    ctx.close();
                }
            });
        } catch (Exception e) {
            MTProtoProxyMod.LOGGER.error("MTProto handshake failed", e);
            ctx.close();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!handshakeDone) {
            // In a real implementation, we would parse server handshake here
            MTProtoProxyMod.LOGGER.warn("Received data before handshake complete, ignoring");
            ((ByteBuf) msg).release();
            return;
        }
        ByteBuf encrypted = (ByteBuf) msg;
        ByteBuf decrypted = decryptor.decrypt(encrypted);
        // Forward decrypted data to ClientConnection
        // ClientConnection expects to receive packets, but raw ByteBuf is not packet.
        // We need to integrate with Minecraft's packet system. This is the hardest part.
        // For simplicity, we will write the decrypted data to a custom pipeline or use reflection.
        // Since full integration is complex, we will simulate by sending to an embedded ClientConnection.
        // In a real mod, you would use a custom PacketListener or mixin to inject.
        MTProtoProxyMod.LOGGER.debug("Decrypted {} bytes", decrypted.readableBytes());
        // TODO: feed decrypted bytes into Minecraft's Netty pipeline
        decrypted.release();
        encrypted.release();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!handshakeDone) {
            super.write(ctx, msg, promise);
            return;
        }
        ByteBuf plain = (ByteBuf) msg;
        ByteBuf encrypted = encryptor.encrypt(plain);
        ctx.write(encrypted, promise);
        plain.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        MTProtoProxyMod.LOGGER.error("MTProto proxy handler error", cause);
        ctx.close();
    }

    private class BackendHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // Data from target server -> MTProto proxy -> client
            if (MTProtoProxyHandler.this.handshakeDone) {
                ByteBuf plain = (ByteBuf) msg;
                ByteBuf encrypted = encryptor.encrypt(plain);
                MTProtoProxyHandler.this.clientConnection.send(encrypted, PacketCallbacks.always(() -> {}));
                plain.release();
            } else {
                ((ByteBuf) msg).release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            MTProtoProxyMod.LOGGER.error("Backend connection error", cause);
            MTProtoProxyHandler.this.clientConnection.disconnect();
            MTProtoProxyHandler.this.outboundChannel.close();
        }
    }
}