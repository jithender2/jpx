package com.proxy.netty.codec.detector;

import com.proxy.netty.codec.handler.ServerSSLContextManager;
import com.proxy.listener.MessageListener;
import com.proxy.netty.codec.handler.Socks4ProxyHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.proxy.ProxyHandler;
import java.util.function.Supplier;

import io.netty.handler.codec.socksx.v4.Socks4ServerDecoder;
import io.netty.handler.codec.socksx.v4.Socks4ServerEncoder;
import io.netty.channel.ChannelPipeline;

public class Socks4ProxyMatcher extends ProtocolMatcher {
    
    private final MessageListener messageListener;
    private final ServerSSLContextManager sslContextManager;
    private final Supplier<ProxyHandler> proxyHandlerSupplier;

    public Socks4ProxyMatcher(MessageListener messageListener, ServerSSLContextManager sslContextManager,
                              Supplier<ProxyHandler> proxyHandlerSupplier) {
        this.messageListener = messageListener;
        this.sslContextManager = sslContextManager;
        this.proxyHandlerSupplier = proxyHandlerSupplier;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return PENDING;
        }
        byte first = buf.getByte(buf.readerIndex());
        byte second = buf.getByte(buf.readerIndex() + 1);
        if (first == 4 && second == 1) {
            return MATCH;
        }
        return MISMATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(Socks4ServerEncoder.INSTANCE);
        pipeline.addLast(new Socks4ServerDecoder());
        pipeline.addLast(new Socks4ProxyHandler(messageListener, sslContextManager, proxyHandlerSupplier));
    }
}