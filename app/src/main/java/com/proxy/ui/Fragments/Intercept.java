package com.proxy.ui.Fragments;

import android.graphics.Color;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.proxy.R;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import com.proxy.data.Http1Message;
import com.proxy.data.Http1RequestHeaders;
import com.proxy.data.Http2Message;
import com.proxy.data.Header;
import com.proxy.data.Http2RequestHeaders;
import com.proxy.data.RequestLine;
import com.proxy.listener.BodyListener;
import com.proxy.listener.HttpInterceptorListener;
import com.proxy.netty.codec.frame.IHttp2HeadersEvent;
import com.proxy.store.Body;
import com.proxy.listener.Http2InterceptorListener;
import com.proxy.listener.HttpMessageListener;
import com.proxy.listener.SetLogger;
import com.proxy.utils.BodyType;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import java.io.InputStreamReader;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.nio.charset.Charset;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Intercept extends Fragment {
	Button buttonIntercept, buttonForward, edit;
	EditText editTextRequest;
	private final Queue<RequestEntry> requestQueue = new ConcurrentLinkedQueue<>();
	private final Queue<HttpRequestEntry> httpRequestEntryQueue = new ConcurrentLinkedQueue<>();
	int streamId;
	private boolean isProcessing = false;

	private final ExecutorService executorService = Executors.newFixedThreadPool(2);

	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		View view = arg0.inflate(R.layout.interceptor, arg1, false);
		init(view);

		listener();
		clickListeners();

		return view;
	}

	private void init(View view) {
		buttonIntercept = view.findViewById(R.id.interceptor_button_intercept);
		buttonForward = view.findViewById(R.id.interceptor_button_forward);
		editTextRequest = view.findViewById(R.id.interceptor_editText);
		edit = view.findViewById(R.id.interceptor_button_edit);

	}

	/**
	* Sets up click listeners for the UI buttons.
	*
	* This method initializes click listeners for the intercept, forward, and edit buttons.
	* The intercept button toggles HTTP and HTTP/2 interception, the forward button forwards
	* requests based on their format (HTTP/1 or HTTP/2), and the edit button enables editing
	* of the request text.
	*/
	private void clickListeners() {
		buttonIntercept.setOnClickListener(v -> {
			// Handle HttpInterceptorListener
			executorService.execute(() -> {
				try {
					processPendingRequests(); // Forward any pending requests before toggling.
					toggleHttpInterception();
					toggleHttp2Interception();
					updateButtonText();
				} catch (Exception e) {
					handleError(e);
				}
			});

		});

		buttonForward.setOnClickListener(v -> {
			String requestText = editTextRequest.getText().toString();
			if (requestText.startsWith(":")) {
				processHttp2Request();
			} else {
				processHttp1Request();
			}
		});

		edit.setOnClickListener(v -> {
			enableEditing(editTextRequest);
		});
	}

	/**
	 * Enables editing for the given EditText.
	 *
	 * This method makes the EditText focusable, displays the cursor, enables text selection,
	 * and restores the default key listener.  This effectively makes the EditText editable.
	 *
	 * @param editText The EditText to enable editing for.
	 */
	private void enableEditing(EditText editText) {
		editText.setFocusableInTouchMode(true);
		editText.setCursorVisible(true);
		editText.setTextIsSelectable(true);
		editText.setKeyListener(new EditText(getContext()).getKeyListener()); // Restore default key listener.
	}

	/**
	 * Processes any pending HTTP requests before interception is toggled.
	 *
	 * This method checks both the `requestQueue` (HTTP/2) and `httpRequestEntryQueue` (HTTP/1)
	 * for pending requests. If any requests are found, it calls `forwardAllRemainingRequests()` to
	 * forward them before interception is toggled. This ensures that no requests are lost during
	 * the interception toggle.
	 */
	private void processPendingRequests() {
		if (!requestQueue.isEmpty() || !httpRequestEntryQueue.isEmpty()) {
			forwardAllRemainingRequests();
		}
	}

	/**
	 * Toggles HTTP interception using the HttpInterceptorListener.
	 */
	private void toggleHttpInterception() {
		HttpInterceptorListener.getInstance().toggleInterception();
	}

	/**
	 * Toggles HTTP/2 interception using the Http2InterceptorListener.
	 */
	private void toggleHttp2Interception() {
		Http2InterceptorListener.getInstance().toggleInterception();
	}

	/**
	 * Updates the text of the intercept button based on the interception state.
	 *
	 * This method checks the interception state of both HTTP and HTTP/2 interceptors and updates
	 * the text of the `buttonIntercept` to "ON" or "OFF" accordingly.  The UI update is performed
	 * on the main thread using a Handler.
	 */
	private void updateButtonText() {
		new Handler(Looper.getMainLooper()).post(() -> {
			boolean interceptionOn = HttpInterceptorListener.getInstance().isInterceptionOn()
					&& Http2InterceptorListener.getInstance().isInterceptionOn();
			buttonIntercept.setText(interceptionOn ? "ON" : "OFF");
		});
	}

	/**
	 * Handles errors that occur during interception toggling.
	 *
	 * This method logs the error and displays a toast message to the user indicating that
	 * the interception toggle failed. The toast is displayed on the main thread using a Handler.
	 *
	 * @param e The exception that occurred.
	 */
	private void handleError(Exception e) {
		Log.e("InterceptionError", "Error during interception toggle", e); // Log the full exception details (including stack trace)
		new Handler(Looper.getMainLooper()).post(() -> {
			Toast.makeText(getContext(), "Failed to toggle interception", Toast.LENGTH_SHORT).show();
		});
	}

	/**
	* Processes the next HTTP/2 request in the queue.
	*
	* This method retrieves the next `RequestEntry` from the `requestQueue`. If the queue is not empty,
	* it forwards the request using `Http2InterceptorListener.forwardOnClick()`, passing the stream ID
	* and the request from the `editTextRequest`. It then sets `isProcessing` to `false` and calls
	* `showNextRequest()` to initiate the processing of the next request (if any). If the queue is
	* empty, it displays a toast message indicating that there are no requests to forward.  The queue
	* is accessed in a synchronized block to prevent concurrent access.
	*/
	private void processHttp2Request() {
		synchronized (requestQueue) {
			if (!requestQueue.isEmpty()) {
				RequestEntry currentRequest = requestQueue.poll();
				Http2InterceptorListener.getInstance().forwardOnClick(currentRequest.getStreamId(),
						editTextRequest.getText().toString());
				isProcessing = false;
				showNextRequest();
			} else {
				editTextRequest.setText("");
				Toast.makeText(getContext(), "No requests to forward", Toast.LENGTH_SHORT).show();
			}
		}
	}

	/**
	* Processes the next HTTP/1 request in the queue.
	*
	* This method retrieves the next `HttpRequestEntry` from the `httpRequestEntryQueue`. If the queue
	* is not empty, it attempts to parse the headers from the `editTextRequest` using `parseHeaders()`.
	* If the headers are valid, it forwards the request using `HttpInterceptorListener.forwardOnClick()`,
	* passing the stream ID and the updated request. It then sets `isProcessing` to `false` and calls
	* `processNextHttpRequest()` to initiate the processing of the next request (if any). If the
	* headers are invalid, it displays a toast message indicating an invalid request format. The queue
	* is accessed in a synchronized block to prevent concurrent access.
	*/
	private void processHttp1Request() {
		synchronized (httpRequestEntryQueue) {
			if (!httpRequestEntryQueue.isEmpty()) {
				HttpRequestEntry httpRequestEntry = httpRequestEntryQueue.poll();
				try {
					Http1RequestHeaders requestHeaders = parseHeaders(editTextRequest.getText().toString());
					HttpInterceptorListener.getInstance().forwardOnClick(streamId, updatedHttpRequest(requestHeaders));
					isProcessing = false;
					processNextHttpRequest();
				} catch (IllegalArgumentException e) {

					Toast.makeText(getContext(), "Invalid request format", Toast.LENGTH_SHORT).show();
				}
			} else {
				editTextRequest.setText("");
			}
		}
	}

	/**
	* Sets up a listener for HTTP messages using `HttpMessageListener`.
	*
	* This method registers a `LogCallBack` with the `HttpMessageListener` to handle incoming HTTP messages.
	* The callback's `onLog()` method is invoked when a new message arrives.  It distinguishes between
	* HTTP/2 and HTTP/1 messages and dispatches them to the appropriate handlers (`handleHttp2Message()`
	* or `handleHttp1Message()`).  Any exceptions during message processing are caught and logged.
	*/
	private void listener() {
		HttpMessageListener.setLogCallBack(new HttpMessageListener.LogCallBack() {
			@Override
			public void onLog(Object msg, int id) {
				try {
					streamId = id;
					if (msg instanceof IHttp2HeadersEvent) {
						handleHttp2Message((IHttp2HeadersEvent) msg, id);
					} else if (msg instanceof Http1Message) {
						handleHttp1Message((Http1Message) msg, id);
					}
				} catch (Exception e) {
					Log.e("HttpMessageListener", "Error processing message: " + e.getMessage());
				}
			}
		});
	}

	/**
	* Handles incoming HTTP/2 messages.
	*
	* This method is called by the `onLog()` callback when an HTTP/2 message arrives.  It extracts the
	* headers and end-of-stream flag from the `IHttp2HeadersEvent`, creates an `Http2RequestHeaders` object,
	* and adds a new `RequestEntry` to the `requestQueue`.  Finally, it calls `processNextRequest()` to
	* process the queued request.
	*
	* @param headersEvent The `IHttp2HeadersEvent` containing the HTTP/2 headers and end-of-stream information.
	* @param id           The ID of the stream.
	*/
	private void handleHttp2Message(IHttp2HeadersEvent headersEvent, int id) {
		Http2RequestHeaders http2RequestHeaders = onRequestHeaders(id, headersEvent.headers(),
				headersEvent.endOfStream());
		requestQueue.offer(new RequestEntry(id, http2RequestHeaders));
		processNextRequest();
	}

	/**
	* Handles incoming HTTP/1 messages.
	*
	* This method is called by the `onLog()` callback when an HTTP/1 message arrives. It extracts the
	* request headers and body from the `Http1Message`, creates an `HttpRequestEntry`, and adds it to the
	* `httpRequestEntryQueue`. It then calls `processNextHttpRequest()` to process the queued request.
	* The `httpRequestEntryQueue` is accessed in a synchronized block to prevent race conditions.
	*
	* @param message The `Http1Message` containing the HTTP/1 headers and body.
	* @param id      The ID of the request.
	*/
	private void handleHttp1Message(Http1Message message, int id) {
		Http1RequestHeaders requestHeaders = message.requestHeader();
		Body body = message.requestBody();

		synchronized (httpRequestEntryQueue) {
			httpRequestEntryQueue.offer(new HttpRequestEntry(id, requestHeaders, body));
			processNextHttpRequest();
		}
	}

	/**
	* Forwards all remaining HTTP requests in the queues (both HTTP/1 and HTTP/2).
	*
	* This method iterates through both the `requestQueue` (HTTP/2) and `httpRequestEntryQueue` (HTTP/1),
	* forwarding each request using the appropriate listener (`Http2InterceptorListener` or
	* `HttpInterceptorListener`).  It clears the queues after forwarding all requests.  Synchronization
	* is used to protect the queues from concurrent access.
	*/
	private void forwardAllRemainingRequests() {
		// Forward remaining HTTP/2 requests
		synchronized (requestQueue) {
			while (!requestQueue.isEmpty()) {
				RequestEntry currentRequest = requestQueue.poll(); // Remove the current request
				Http2InterceptorListener.getInstance().forwardOnClick(currentRequest.getStreamId(),
						String.join("\n", currentRequest.getHeaders().rawLines()));
			}
		}

		// Forward remaining HTTP/1 requests
		synchronized (httpRequestEntryQueue) {
			while (!httpRequestEntryQueue.isEmpty()) {
				HttpRequestEntry httpRequestEntry = httpRequestEntryQueue.poll(); // Remove the current request
				try {
					Http1RequestHeaders requestHeaders = httpRequestEntry.getHeaders();
					Body body = httpRequestEntry.getBody();
					HttpInterceptorListener.getInstance().forwardOnClick(httpRequestEntry.getStreamId(),
							updatedHttpRequest(requestHeaders));
				} catch (IllegalArgumentException e) {
					e.printStackTrace(); // Log any parsing issues
				}
			}
		}
		if (httpRequestEntryQueue.isEmpty() && requestQueue.isEmpty()) {
			new Handler(Looper.getMainLooper()).post(() -> {
				boolean isInterceptionOn = HttpInterceptorListener.getInstance().isInterceptionOn()
						&& Http2InterceptorListener.getInstance().isInterceptionOn();
				buttonIntercept.setText(isInterceptionOn ? "ON" : "OFF");
			});
		}
	}

	/**
	* Processes the next HTTP/2 request if available.
	*
	* This method checks the `requestQueue` for pending HTTP/2 requests. If the queue is not empty and
	* no request is currently being processed (`!isProcessing`), it marks `isProcessing` as `true`
	* and calls `showNextRequest()` to display and forward the request.  The queue is accessed in a
	* synchronized block to prevent concurrent access.
	*/
	private void processNextRequest() {
		synchronized (requestQueue) {
			if (!requestQueue.isEmpty() && !isProcessing) {
				isProcessing = true; // Lock processing
				showNextRequest();
			}
		}
	}

	/**
	* Processes the next HTTP/1 request if available.
	*
	* This method checks the `httpRequestEntryQueue` for pending HTTP/1 requests. If the queue is not
	* empty and no request is currently being processed (`!isProcessing`), it marks `isProcessing`
	* as `true` and calls `showNextHttpRequest()` to display and forward the request. The queue is
	* accessed in a synchronized block to prevent concurrent access.
	*/
	private void processNextHttpRequest() {
		synchronized (httpRequestEntryQueue) {
			if (!httpRequestEntryQueue.isEmpty() && !isProcessing) {
				isProcessing = true; // Lock processing
				showNextHttpRequest();
			}
		}
	}

	/**
	* Displays the next HTTP/2 request in the UI.
	*
	* This method retrieves the next `RequestEntry` from the `requestQueue` *without* removing it
	* (`peek()`). It then updates the `editTextRequest` with the request headers on the main thread
	* using a `Handler`.  The `streamId` is also updated. If the queue is empty, the `editTextRequest`
	* is cleared, and the `streamId` is reset. The queue is accessed in a synchronized block.  The
	* `isProcessing` flag is reset *after* the UI update is complete, ensuring thread safety.
	*/
	private void showNextRequest() {
		synchronized (requestQueue) {
			if (!requestQueue.isEmpty()) {
				RequestEntry nextRequest = requestQueue.peek(); // Get the first request without removing
				new Handler(Looper.getMainLooper()).post(() -> {
					editTextRequest.setText(String.join("\n", nextRequest.getHeaders().rawLines()));
					// Update UI here
				});

				streamId = nextRequest.getStreamId(); // Update the current streamId
			} else {
				// Clear the EditText if there are no more requests
				editTextRequest.setText("");
				streamId = -1; // Reset the streamId
			}
		}
	}

	/**
	* Displays the next HTTP/1 request in the UI.
	*
	* This method retrieves the next `HttpRequestEntry` from the `httpRequestEntryQueue` *without*
	* removing it (`peek()`). It then updates the `editTextRequest` with the request headers and body
	* on the main thread using a `Handler`. The `streamId` is also updated. If the queue is empty,
	* the `editTextRequest` is cleared, and the `streamId` is reset. The queue is accessed in a
	* synchronized block. The `isProcessing` flag is reset *after* the UI update is complete,
	* ensuring thread safety.
	*/
	private void showNextHttpRequest() {
		synchronized (httpRequestEntryQueue) {
			if (!httpRequestEntryQueue.isEmpty()) {
				HttpRequestEntry nextRequest = httpRequestEntryQueue.peek(); // Get the first request without removing
				Http1RequestHeaders requestHeaders = nextRequest.getHeaders();

				new Handler(Looper.getMainLooper()).post(() -> {
					editTextRequest.setText(String.join("\n", requestHeaders.rawLines()));
					// Update UI here
				});

				streamId = nextRequest.getStreamId(); // Update the current streamId
			} else {
				// Clear the EditText if there are no more requests
				editTextRequest.setText("");
				streamId = -1; // Reset the streamId
			}
		}
	}

	/**
	* Parses raw HTTP headers from a string into an `Http1RequestHeaders` object.
	*
	* This method parses the provided `rawHeaders` string, which should contain the HTTP headers
	* in the standard format (request line followed by header lines). It validates the input,
	* splits the string into lines, parses the request line and individual headers, and creates
	* an `Http1RequestHeaders` object.
	*
	* @param rawHeaders The raw HTTP headers as a string.  Must not be null or empty.
	* @return An `Http1RequestHeaders` object representing the parsed headers.
	* @throws IllegalArgumentException If the raw headers are invalid or malformed.
	* @throws IllegalStateException    If the request line is missing.
	*/
	private Http1RequestHeaders parseHeaders(String rawHeaders) {
		if (rawHeaders == null || rawHeaders.trim().isEmpty()) {
			throw new IllegalArgumentException("Raw headers cannot be null or empty");
		}

		List<Header> updatedHeaders = new ArrayList<>();
		String[] lines = rawHeaders.split("\\r?\\n");
		String[] topPart = null;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();

			if (i == 0) {
				// Parse the request line (e.g., "GET /path HTTP/1.1")
				topPart = parseRequestLine(line);
			} else {
				// Parse headers (e.g., "Host: example.com")
				parseHeaderLine(line, updatedHeaders);
			}
		}

		if (topPart == null || topPart.length != 3) {
			throw new IllegalStateException("Request headers missing");
		}

		RequestLine requestLine = new RequestLine(topPart[0], topPart[1], topPart[2]);
		return new Http1RequestHeaders(requestLine, updatedHeaders);
	}

	/**
	* Parses the request line from a string.
	*
	* This method parses the first line of the HTTP headers, which should be the request line
	* (e.g., "GET /path HTTP/1.1"). It splits the line into parts and validates the format.
	*
	* @param line The request line string.
	* @return An array of strings containing the method, path, and HTTP version.
	* @throws IllegalArgumentException If the request line is invalid or malformed.
	*/
	private String[] parseRequestLine(String line) {
		String[] parts = line.split(" ");
		if (parts.length != 3 || !parts[2].startsWith("HTTP/")) {
			throw new IllegalArgumentException("Invalid request line: " + line);
		}
		return parts;
	}

	/**
	* Parses a single header line and adds it to the list of headers.
	*
	* This method parses a single header line (e.g., "Host: example.com"). It splits the line
	* into name and value parts, trims whitespace, and creates a `Header` object.
	*
	* @param line    The header line string.
	* @param headers The list to which the parsed header should be added.
	* @throws IllegalArgumentException If the header line is invalid or malformed.
	*/
	private void parseHeaderLine(String line, List<Header> headers) {
		String[] headerParts = line.split(":", 2);
		if (headerParts.length == 2) {
			String name = headerParts[0].trim();
			String value = headerParts[1].trim();
			if (!name.isEmpty() && !value.isEmpty()) {
				headers.add(new Header(name, value));
			} else {
				throw new IllegalArgumentException("Invalid header: " + line);
			}
		} else {
			throw new IllegalArgumentException("Invalid header format: " + line);
		}

	}

	/**
	* Creates an `HttpRequest` object from `Http1RequestHeaders`, ensuring the Host header is present.
	*
	* This method takes `Http1RequestHeaders` as input and constructs an `HttpRequest` object.
	* It extracts the HTTP version, method, and URI from the headers and sets them in the new request.
	* It then copies all headers from the `Http1RequestHeaders` to the `HttpRequest`.  Critically, it
	* ensures that the `Host` header is present. If it's missing in the provided headers, it uses
	* the host extracted earlier to set it.
	*
	* @param http1RequestHeaders The `Http1RequestHeaders` object containing the request headers.
	* @return An `HttpRequest` object representing the request.
	* @throws IllegalArgumentException If the provided headers are invalid.
	* @throws IllegalStateException    If the Host header is missing and cannot be determined.
	*/
	private HttpRequest updatedHttpRequest(Http1RequestHeaders http1RequestHeaders) {
		if (http1RequestHeaders == null) {
			throw new IllegalArgumentException("Invalid headers");
		}

		// Ensure the Host header is set
		String host = http1RequestHeaders.headers().stream().filter(header -> "Host".equalsIgnoreCase(header.name()))
				.map(Header::value).findFirst().orElse(null);

		if (host == null || host.isEmpty()) {
			throw new IllegalStateException("Missing Host header");
		}

		HttpVersion version = HttpVersion.valueOf(http1RequestHeaders.requestLine().version());
		HttpMethod method = HttpMethod.valueOf(http1RequestHeaders.requestLine().method());
		String uri = http1RequestHeaders.requestLine().path();

		HttpRequest request = new DefaultHttpRequest(version, method, uri);
		for (Header header : http1RequestHeaders.headers()) {
			request.headers().set(header.name(), header.value());
		}

		// Ensure the Host header is set (if not already in the headers)
		if (!request.headers().contains("Host")) {
			request.headers().set("Host", host);
		}

		return request;
	}

	/**
	* Extracts the path from an HTTP request string.
	*
	* This method extracts the path part (the URI) from the first line of an HTTP request string.
	* It splits the string into lines, takes the first line, and then splits that line into parts
	* separated by spaces.  The second part is assumed to be the path.
	*
	* @param httpRequest The HTTP request string.
	* @return The path part of the request, or null if it cannot be extracted.
	*/
	public static String extractPath(String httpRequest) {
		// Extract the first line and get the path part
		String firstLine = httpRequest.split("\n")[0];
		String[] parts = firstLine.split(" ");
		return parts.length > 1 ? parts[1] : null; // Return the second part (the path)
	}

	/**
	* Creates an `Http2RequestHeaders` object from Netty's `Http2Headers`.
	*
	* This method converts Netty's `Http2Headers` object to a custom `Http2RequestHeaders` object.
	* It iterates through the Netty headers, converts them to `Header` objects, and uses them
	* to create the `Http2RequestHeaders` object.
	*
	* @param streamId      The ID of the stream.  (Consider documenting the purpose of streamId).
	* @param http2Headers  The Netty `Http2Headers` object.
	* @param endOfStream   A boolean indicating if this is the end of the stream. (Consider documenting the purpose of endOfStream).
	* @return An `Http2RequestHeaders` object.
	*/
	private Http2RequestHeaders onRequestHeaders(int streamId, Http2Headers http2Headers, boolean endOfStream) {

		List<Header> headers = convertHeaders(http2Headers);
		Http2RequestHeaders requestHeaders = new Http2RequestHeaders(headers, http2Headers.scheme().toString(),
				http2Headers.method().toString(), http2Headers.path().toString());

		return requestHeaders;
	}

	/**
	* Converts Netty's `Http2Headers` to a list of `Header` objects.
	*
	* This method iterates through the Netty `Http2Headers` entries and creates a `Header` object
	* for each entry.  It handles potential null keys.
	*
	* @param nettyHeaders The Netty `Http2Headers` object.
	* @return A list of `Header` objects.
	*/
	private List<Header> convertHeaders(Http2Headers nettyHeaders) {
		ArrayList<Header> http2HeadersArrayList = new ArrayList<>();
		for (Map.Entry next : nettyHeaders) {
			if (next.getKey() != null) {
				http2HeadersArrayList.add(new Header(next.getKey().toString(), next.getValue().toString()));
			}
		}
		return http2HeadersArrayList;
	}

	/**
	* Converts a Netty `HttpRequest` to a custom `Http1RequestHeaders` object.
	*
	* This method takes a Netty `HttpRequest` object and extracts the request line (method, URI,
	* and HTTP version) and headers to create a custom `Http1RequestHeaders` object.
	*
	* @param request The Netty `HttpRequest` object.
	* @return An `Http1RequestHeaders` object representing the request headers.
	*/
	private static Http1RequestHeaders converHttpHeaders(HttpRequest request) {

		RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
				request.protocolVersion().text());
		List<Header> headers = new ArrayList<>();

		request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
		return new Http1RequestHeaders(requestLine, headers);
	}

	/**
	* Represents an HTTP/2 request entry, storing the stream ID and headers.
	*/
	public class RequestEntry {
		/**
		* Constructs a new `RequestEntry`.
		*
		* @param streamId The ID of the HTTP/2 stream.
		* @param headers  The HTTP/2 request headers.
		*/
		private final int streamId;
		private final Http2RequestHeaders headers;

		public RequestEntry(int streamId, Http2RequestHeaders headers) {
			this.streamId = streamId;
			this.headers = headers;
		}

		/**
		 * Returns the stream ID.
		 *
		 * @return The stream ID.
		 */

		public int getStreamId() {
			return streamId;
		}

		public Http2RequestHeaders getHeaders() {
			return headers;
		}
	}

	/**
	* Represents an HTTP/1 request entry, storing the stream ID, headers, and body.
	*/
	public class HttpRequestEntry {
		private final int streamId;
		private final Http1RequestHeaders headers;
		private final Body body; // Make body final as well for immutability.

		/**
		 * Constructs a new `HttpRequestEntry`.
		 *
		 * @param streamId The ID of the HTTP/1 request.
		 * @param headers  The HTTP/1 request headers.
		 * @param body     The HTTP request body.
		 */
		public HttpRequestEntry(int streamId, Http1RequestHeaders headers, Body body) {
			this.streamId = streamId;
			this.headers = headers;
			this.body = body;
		}

		/**
		 * Returns the stream ID.
		 *
		 * @return The stream ID.
		 */
		public int getStreamId() {
			return streamId;
		}

		/**
		 * Returns the HTTP/1 request headers.
		 *
		 * @return The HTTP/1 request headers.
		 */
		public Http1RequestHeaders getHeaders() {
			return headers;
		}

		/**
		 * Returns the HTTP request body.
		 *
		 * @return The HTTP request body.
		 */
		public Body getBody() {
			return body;
		}
	}

	/**
	 * Shuts down the executor service when the component is destroyed.
	 *
	 * This method is called when the component is being destroyed. It checks if the `executorService`
	 * is not null and is not already shut down, and then shuts it down.  This prevents resource leaks.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown(); // Initiate shutdown (waits for tasks to complete).
			// Or use shutdownNow() to interrupt running tasks if needed:
			// executorService.shutdownNow();
		}
	}

}
