package com.github.mtprotoproxy.mixin;

import com.github.mtprotoproxy.ConfigManager;
import com.github.mtprotoproxy.proxy.MTProtoProxyInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "connect", at = @At("HEAD"), cancellable = true)
    private static void onConnect(InetSocketAddress address, boolean useEpoll, CallbackInfo ci) {
        if (ConfigManager.isProxyEnabled() && ConfigManager.getSecret() != null) {
            // Replace connection with MTProto proxy tunnel
            MTProtoProxyMod.LOGGER.info("Redirecting connection to {} through MTProto proxy {}:{}",
                    address, ConfigManager.getProxyHost(), ConfigManager.getProxyPort());

            // Use custom channel initializer to wrap socket
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(MTProtoProxyInitializer.getWorkerGroup());
            bootstrap.channel(MTProtoProxyInitializer.getSocketChannelClass());
            bootstrap.handler(new MTProtoProxyInitializer(address));
            bootstrap.connect(new InetSocketAddress(ConfigManager.getProxyHost(), ConfigManager.getProxyPort()))
                    .addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            MTProtoProxyMod.LOGGER.error("Failed to connect to MTProto proxy", future.cause());
                        }
                    });
            ci.cancel();
        }
    }
}