package com.proxy.netty.codec.detector;

import com.proxy.netty.codec.web.HttpRequestHandler;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.channel.ChannelPipeline;
import com.proxy.netty.codec.handler.ServerSSLContextManager;
import java.util.Set;
import io.netty.buffer.ByteBuf;
import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Matcher for plain http request.
 */
public class HttpMatcher extends ProtocolMatcher {
    
    private static Set<String> methods = Set.of("GET", "POST", "PUT", "HEAD", "OPTIONS", "PATCH", "DELETE",
            "TRACE");

    private final ServerSSLContextManager sslContextManager;

    public HttpMatcher(ServerSSLContextManager sslContextManager) {
        this.sslContextManager = sslContextManager;
    }

    @Override
    public int match(ByteBuf buf) {
        if (buf.readableBytes() < 8) {
            return PENDING;
        }

        int index = buf.indexOf(0, 8, (byte) ' ');
        if (index < 0) {
            return MISMATCH;
        }

        int firstURIIndex = index + 1;
        if (buf.readableBytes() < firstURIIndex + 1) {
            return PENDING;
        }

        String method = buf.toString(0, index, US_ASCII);
        char firstURI = (char) (buf.getByte(firstURIIndex + buf.readerIndex()) & 0xff);
        if (!methods.contains(method) || firstURI != '/') {
            return MISMATCH;
        }

        return MATCH;
    }

    @Override
    public void handlePipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpServerExpectContinueHandler());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpContentCompressor());
        pipeline.addLast(new HttpRequestHandler(sslContextManager));
    }
}