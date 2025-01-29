package com.proxy.listener;

import com.proxy.netty.codec.frame.Http2PriorityHeadersEvent;
import com.proxy.netty.codec.frame.IHttp2HeadersEvent;

import com.proxy.store.Body;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * `Http2InterceptorListener` is a singleton class responsible for intercepting and
 * modifying HTTP/2 requests. It allows pausing requests, modifying headers, and
 * forwarding them based on user interaction.  This class is designed to be
 * used as part of an HTTP/2 proxy or interception tool.
 *
 * <p>This class is thread-safe due to the use of concurrent data structures
 * and synchronization where necessary.</p>
 * This handles  headers only body interception yet and modification not implemented yet
 */
public class Http2InterceptorListener {
	private static Http2InterceptorListener instance;
	private boolean interceptionOn = false;
	//	private volatile boolean latchAwaiting = false;
	// Map to store the pending request data for each stream
	private ConcurrentHashMap<Integer, InterceptedRequest> interceptedRequests = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, CountDownLatch> countDownLatchMap = new ConcurrentHashMap<>();
	Body body = null;

	public static synchronized Http2InterceptorListener getInstance() {
		if (instance == null) {
			instance = new Http2InterceptorListener();
		}
		return instance;
	}

	public synchronized void toggleInterception() {
		interceptionOn = !interceptionOn;
	}

	public boolean isInterceptionOn() {
		return interceptionOn;
	}

	// Store pending request for later forwarding
	public void storePendingRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, int streamId) {
		interceptedRequests.put(streamId, new InterceptedRequest(ctx, msg, promise));

		HttpMessageListener.log(msg, streamId);

		CountDownLatch latch = new CountDownLatch(1);
		countDownLatchMap.put(streamId, latch);

		CountDownLatch checkLatch = countDownLatchMap.get(streamId);

		try {
			//SetLogger.log("Awaiting latch for Stream ID: " + streamId);
			latch.await();

			forward(streamId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			//	SetLogger.log("Latch await interrupted for Stream ID: " + streamId);
		}
	}
	/**
     * Forwards a stored request.  This method is called after the request is
     * unblocked (e.g., by `forwardOnClick`).
     *
     * @param streamId The stream ID of the request to forward.
     */
	public void forward(int streamId) {
		InterceptedRequest interceptedRequest = interceptedRequests.remove(streamId);
		if (interceptedRequest != null) {
			ChannelHandlerContext ctx = interceptedRequest.getCtx();
			Object msg = interceptedRequest.getMsg();
			ChannelPromise promise = interceptedRequest.getPromise();
			ctx.write(msg, promise).addListener(future -> {
				if (!future.isSuccess()) {
					//Todo throw a error to show the request was failed
				}

			});
		} else {

		}
	}

	// This released the latch allowing the request to be forwarded
	public void forwardOnClick(int streamId) {
		CountDownLatch latch = countDownLatchMap.get(streamId);
		if (latch != null) {
			latch.countDown();
			countDownLatchMap.remove(streamId);

		}

	}

	/**
	 * Unblocks a paused request and forwards it with modified headers. 
	 * @param streamId        The stream ID of the request to unblock.
	 * @param modifiedHeaders The modified HTTP/2 headers.
	 */

	public void forwardOnClick(int streamId, Http2Headers modifiedHeaders) {
		InterceptedRequest interceptedRequest = interceptedRequests.get(streamId);

		if (interceptedRequest != null) {

			IHttp2HeadersEvent headersEvent = (IHttp2HeadersEvent) interceptedRequest.getMsg();
			IHttp2HeadersEvent newHeadersEvent = new Http2PriorityHeadersEvent(streamId, modifiedHeaders,
					headersEvent.padding(), headersEvent.endOfStream(),
					((Http2PriorityHeadersEvent) headersEvent).streamDependency(),
					((Http2PriorityHeadersEvent) headersEvent).weight(),
					((Http2PriorityHeadersEvent) headersEvent).exclusive());
			interceptedRequest.updateMsg(newHeadersEvent);

		}

		forwardOnClick(streamId);
	}

	/**
	 * Unblocks a paused request and forwards it with modified headers provided
	 * as a text string. This method parses the text string into `Http2Headers`
	 * before forwarding.  The text should be formatted with one header per line,
	 * in the format "Header-Name: Header-Value".
	 *this method executed by user interection
	 * @param streamId           The stream ID of the request to unblock.
	 * @param modifiedHeadersText The modified HTTP/2 headers as a text string.
	 */
	public void forwardOnClick(int streamId, String modifiedHeadersText) {
		// Split the modified headers into individual lines
		String[] headerLines = modifiedHeadersText.split("\n");
		Http2Headers modifiedHeaders = new DefaultHttp2Headers();

		String scheme = null;
		String method = null;
		String path = null;
		String authority = null;

		for (String line : headerLines) {
			line = line.trim(); // Remove leading/trailing whitespaces

			// Skip empty or malformed lines
			if (line.isEmpty() || !line.contains(":")) {
				continue;
			}

			// Find the index of the first colon after the first character
			int idx = line.substring(1).indexOf(":") + 1;
			if (idx <= 0) {
				continue; // Invalid header line, skip it
			}

			String name = line.substring(0, idx).trim();
			String value = line.substring(idx + 1).trim();

			if (!name.isEmpty() && !value.isEmpty()) {
				modifiedHeaders.add(name, value);

				// Handle pseudo-headers
				switch (name) {
				case ":scheme":
					scheme = value;
					break;
				case ":method":
					method = value;
					break;
				case ":path":
					path = value;
					break;
				case ":authority":
					authority = value;
					break;
				}
			}
		}

		// Ensure pseudo-headers are set (optional defaults can be applied)
		if (scheme != null)
			modifiedHeaders.scheme(scheme);
		if (method != null)
			modifiedHeaders.method(method);
		if (path != null)
			modifiedHeaders.path(path);
		if (authority != null)
			modifiedHeaders.authority(authority);

		// Call the existing method to forward the request
		forwardOnClick(streamId, modifiedHeaders);
	}

	// Nested class to store intercepted request data
	private static class InterceptedRequest {
		private final ChannelHandlerContext ctx;
		private Object msg;
		private final ChannelPromise promise;

		public InterceptedRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
			this.ctx = ctx;
			this.msg = msg;
			this.promise = promise;

		}

		public ChannelHandlerContext getCtx() {
			return ctx;
		}

		public Object getMsg() {
			return msg;
		}

		public ChannelPromise getPromise() {
			return promise;
		}

		public void updateMsg(Object newMsg) {
			this.msg = newMsg;
		}
	}

}