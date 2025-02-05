package com.proxy.ui.Fragments;

import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.os.Handler;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.proxy.AppContext;
import com.proxy.R;
import android.view.View;
import android.view.LayoutInflater;
import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.Beautifier;
import com.proxy.beautifier.FormEncodedBeautifier;
import com.proxy.data.ContentType;
import com.proxy.data.Header;
import com.proxy.data.Http1Message;
import com.proxy.data.Http1ResponseHeaders;
import com.proxy.data.Http1RequestHeaders;
import com.proxy.data.Http2Message;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import com.proxy.ViewModel.SharedViewModel;
import android.os.Bundle;
import com.proxy.data.RequestLine;
import com.proxy.data.StatusLine;
import com.proxy.listener.MessageListener;
import com.proxy.listener.SetLogger;
import com.proxy.netty.ProxyHandlerSupplier;
import com.proxy.netty.codec.detector.HttpConnectProxyMatcher;
import com.proxy.netty.codec.detector.ProtocolDetector;
import com.proxy.netty.codec.CloseTimeoutChannelHandler;
import com.proxy.netty.codec.handler.HttpConnectProxyInitializer;
import com.proxy.netty.codec.handler.ServerSSLContextManager;
import com.proxy.setting.KeyStoreSetting;
import com.proxy.utils.Chunk;
import com.proxy.utils.DataStore;
import com.proxy.utils.DataStoreManager;
import com.proxy.utils.DataStoreManager;
import com.proxy.utils.HtmlHighlighter;
import com.proxy.utils.Networks;
import io.netty.buffer.ByteBuf;
import com.proxy.store.Body;
import com.proxy.utils.BodyType;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import com.proxy.data.HttpMessage;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

import io.netty.util.concurrent.GenericFutureListener;
import java.io.InputStreamReader;
import java.io.Reader;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.HttpURLConnection;
import javax.net.ssl.SSLException;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import javax.xml.parsers.DocumentBuilder;
import java.util.zip.Inflater;
import javax.xml.parsers.DocumentBuilderFactory;
import org.tukaani.xz.LZMAInputStream;
import org.xml.sax.InputSource;
import org.brotli.dec.BrotliInputStream;
import java.util.zip.InflaterInputStream;

/**
 * A fragment or view responsible for replaying HTTP requests and displaying responses.
 * Contains UI elements for request input, response display, search, and navigation.
 */
public class Repeator extends Fragment {
	SharedViewModel sharedViewModel; // ViewModel for sharing data between components.
	Button button; // Button to send the HTTP request.
	EditText requestTextView; // EditText for inputting the HTTP request.
	EditText repeaterSearchText; // EditText for searching within the response.
	TextView responseView; // TextView to display the HTTP response.
	private List<Pair<Integer, Integer>> matchPositions = new ArrayList<>(); // Positions of search matches.
	ScrollView scrollView; // ScrollView for the response display.
	ImageView forward; // Button to navigate to the next search match.
	ImageView backward; // Button to navigate to the previous search match.
	ImageView expand; // Button to expand/collapse a view (e.g., a detailed view).
	FrameLayout frameLayout; // Container for expandable content.
	int currentMatchIndex = 0; // Index of the currently highlighted search match.

	
	private static final int DEFAULT_CHUNK_SIZE = 10000; // Default chunk size for large responses.
	private List<String> textChunks; // List to store chunks of the response text.
	private int currentChunkIndex = 0; // Index of the currently displayed chunk.
	SslContext sslContext = null; // SSL context for secure connections.

	// Header names (Good practice to define constants).
	private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
	private static final String CONTENT_TYPE_HEADER = "Content-Type"; // Corrected capitalization
	private static final String CONTENT_LENGTH_HEADER = "Content-Length"; // Corrected capitalization

	private static final List<Beautifier> beautifiers = List.of(new FormEncodedBeautifier(), new XmlBeautifier(),
			new HtmlBeautifier()); // List of beautifiers for formatting responses.

	String fullresponse = ""; // Stores the full response string.  Consider renaming to fullResponse
	String contentEncoding; // Stores the Content-Encoding of the response.
	private int PORT = 443; // default port number

	public Repeator() {
	}

	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		View view = arg0.inflate(R.layout.repeator, arg1, false);
		init(view);

		sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
		sharedViewModel.getRepeaterRequests().observe(getViewLifecycleOwner(), new Observer<HttpMessage>() {
			@Override
			public void onChanged(HttpMessage arg0) {
				if (arg0 != null) {
					setHttpMessage(arg0);
					SetLogger.log(arg0.url());
					requestTextView.setText(highlightAndFormatHttpRequest(requestTextView.getText().toString()));

				}
			}
		});

		return view;
	}

	/**
	* Initializes the UI elements and sets up click listeners.
	*
	* @param view The root View of the layout containing these elements.  Must not be null.
	* @throws NullPointerException If the provided View is null.
	*/
	private void init(View view) {
		button = view.findViewById(R.id.repeater_button);
		requestTextView = view.findViewById(R.id.repeater_request);
		responseView = view.findViewById(R.id.repeater_response);
		backward = view.findViewById(R.id.repeater_previous_arrow);
		forward = view.findViewById(R.id.repeater_next_arrow);
		repeaterSearchText = view.findViewById(R.id.repeater_search);
		scrollView = view.findViewById(R.id.repeater_scroll_view);
		frameLayout = view.findViewById(R.id.repeater_bottom_frame);
		expand = view.findViewById(R.id.repeater_show_full_screen);
		setupButtons();
	}

	/**
	* Sets the HTTP message content in the request text view.
	*
	* @param message The HttpMessage object containing the request. Must not be null.
	* @throws NullPointerException If the provided HttpMessage is null.
	*/
	private void setHttpMessage(HttpMessage message) {
		Body body = message.requestBody();
		String reqeust = String.join("\n", message.requestHeader().rawLines());
		requestTextView.setText(reqeust + "\n\n" + getBody(body));

	}

	/**
	* Sends an HTTP request based on the provided raw request string.
	* Determines whether it's an HTTP/2 or HTTP/1.1 request based on the request format.
	*
	* @param rawRequest The raw request string, which can be either HTTP/2 (starting with ":")
	*                   or HTTP/1.1. Must not be null or empty.
	*/
	private void sendRequest(String rawRequest) {
		if (!rawRequest.isEmpty()) {

			if (rawRequest.startsWith(":")) {
				sendHttp2Request(rawRequest);
			} else {
				String host = extractHost(rawRequest);
				try {
					HttpRequest request = constructRequest(rawRequest);
					replayRequest(constructRequest(rawRequest), host);
				} catch (IllegalArgumentException e) {
					showToast(e.getMessage());
				}
			}
		}

	}

	/**
	* Formats HTTP headers from a Map to a String representation.
	* Handles cases where headers have multiple values and includes the status line.
	*
	* @param headerFields A Map where keys are header names and values are Lists of header values.
	*                     The status line (e.g., "HTTP/1.1 200 OK") is represented by a null key.
	* @return A String containing the formatted headers and status line, or an empty string
	*         if the input Map is null or empty.
	*/
	private String formatHeaders(Map<String, List<String>> headerFields) {
		if (headerFields == null || headerFields.isEmpty()) {
			return ""; // Return empty string if no headers are present
		}

		StringBuilder stringBuilder = new StringBuilder();
		headerFields.forEach((headerName, headerValues) -> {
			if (headerValues == null || headerValues.isEmpty()) {
				return; // Skip empty header values
			}

			if (headerName != null) {
				// Join multiple header values with a comma
				String combinedValues = String.join(", ", headerValues);
				stringBuilder.append(headerName).append(": ").append(combinedValues).append("\n");
			} else {
				// Handle status line (e.g., HTTP version and status code)
				headerValues.forEach(value -> stringBuilder.append(value).append("\n"));
			}
		});

		return stringBuilder.toString();
	}

	/**
	* Displays a Toast message on the UI thread.
	*
	* @param message The message to display in the Toast.
	*/
	private void showToast(String message) {
		getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show());
	}

	/**
	 * Reads all data from an InputStream and returns it as a String.
	 *
	 * @param inputStream The InputStream to read from.  Should not be null.
	 * @return A String containing the data read from the InputStream, or an empty string
	 *         if an IOException occurs.
	 * @throws NullPointerException If the inputStream is null.
	 */
	private static String readStreams(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			response.append(line.trim()).append("\n");
		}
		reader.close();
		return response.toString();
	}

	/**
	* Converts a raw request string (containing pseudo-headers and headers) into an HttpURLConnection.
	*
	* @param request The raw request string, formatted with pseudo-headers (e.g., :scheme, :method, :path, :authority)
	*                followed by regular headers and an optional body separated by an empty line.
	* @return An HttpURLConnection object configured with the parsed headers and body, or null if the request is invalid.
	*/
	private HttpURLConnection convertToHeaders(String request) {
		String[] headerLines = request.split("\n");
		HttpURLConnection connection = null;

		String scheme = null;
		String method = null;
		String path = null;
		String authority = null;

		StringBuilder requestBody = new StringBuilder();
		boolean isRequestBody = false;

		// Parse the pseudo-headers and headers
		for (String line : headerLines) {
			line = line.trim();

			if (line.isEmpty()) {
				isRequestBody = true; // Body starts after an empty line
				continue;
			}

			if (!isRequestBody) {
				if (!line.contains(":")) {
					continue; // Skip malformed lines
				}

				int idx = line.substring(1).indexOf(":") + 1;
				if (idx <= 0) {
					continue;
				}

				String name = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();

				if (!name.isEmpty() && !value.isEmpty()) {
					switch (name) {
					case ":scheme":
						scheme = value;
						break;
					case ":method":
						method = value.toUpperCase();
						break;
					case ":path":
						path = value;
						break;
					case ":authority":
						authority = value;
						break;
					}
				}
			} else {
				// Accumulate the request body after the empty line
				requestBody.append(line).append("\n");
			}
		}

		// Validate essential pseudo-headers
		if (scheme == null || authority == null || path == null || method == null) {
			showToast("Invalid request format: Missing required pseudo-headers.");
			return null;
		}

		try {
			// Construct the URL
			String urlString = scheme + "://" + authority + path;
			URL url = new URL(urlString);

			// Open the connection
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);

			// Add regular headers
			for (String line : headerLines) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith(":")) {
					continue; // Skip pseudo-headers and empty lines
				}

				int idx = line.indexOf(":");
				if (idx > 0) {
					String name = line.substring(0, idx).trim();
					String value = line.substring(idx + 1).trim();
					connection.setRequestProperty(name, value);
					
				}
			}

			// Handle request body for methods that allow it
			if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE")) {
				connection.setDoOutput(true); // Enable writing to the connection
				if (requestBody.length() > 0) {
					connection.setRequestProperty("Content-Length",String.valueOf(requestBody.length()));
					
						//connection.setFixedLengthStreamingMode(requestBody.toString().trim().getBytes("UTF-8").length);
					try (OutputStream outputStream = connection.getOutputStream()) {
						outputStream.write(requestBody.toString().getBytes("UTF-8"));
						outputStream.flush();
					} catch (Exception e) {
						showToast(e.getMessage());
					}
				}
			}

		} catch (IOException | IllegalArgumentException e) {
			showToast(e.getMessage());
			e.printStackTrace();
		}

		return connection;
	}

	/**
	* Sends an HTTP/2 request (or potentially HTTP/1.1, depending on the server) and handles the response.
	*
	* @param request The raw request string (which will be converted to headers).
	*/
	
	//Yet to implement thia functionality in netty,this current function could be buggy
	public void sendHttp2Request(String request) {
		if (request.isEmpty()) {
			showToast("Empty request");
			return;
		}
		Thread thread = new Thread(() -> {
			HttpURLConnection connection = convertToHeaders(request);

			if (connection == null) {

				showToast("Invalid request");
				return;
			}
			try {
				// Open connection and get the response code
				int responseCode = connection.getResponseCode();

				// Update the response view with response code
				// updateResponseView(responseCode + "");

				StringBuilder stringBuilder = new StringBuilder();
				String headers = formatHeaders(connection.getHeaderFields());

				// Read the initial status line to extract the HTTP version
				String statusLine = connection.getHeaderField(0); // The first field contains the status line
				String httpVersion = null;

				if (statusLine != null && statusLine.startsWith("HTTP")) {
					// Extract the HTTP version (e.g., HTTP/1.1, HTTP/2)
					String[] statusParts = statusLine.split(" ");
					if (statusParts.length > 0) {
						httpVersion = statusParts[0]; // HTTP/1.1 or HTTP/2
					}
				}
				StringBuilder fullResponse = new StringBuilder();

				// Handle the response body
				InputStream inputStream = connection.getInputStream();
				String encoding = connection.getContentEncoding();
				InputStream stream = getDecodedInputStream(inputStream, encoding);

				String text = Body.readAll(new InputStreamReader(stream, CharsetUtil.UTF_8));

				fullresponse = headers + "\n" + text;
				startLazyLoad(fullresponse);

				// Update the response view with formatted request and body

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (connection != null) {
					connection.disconnect();
				}
			}

		});

		thread.start(); // Run the network request on a separate thread

	}

	public static String extractPath(String httpRequest) {
		// Extract the first line and get the path part
		String firstLine = httpRequest.split("\n")[0];
		String[] parts = firstLine.split(" ");
		return parts.length > 1 ? parts[1] : null; // Return the second part (the path)
	}

	public static String extractHost(String httpRequest) {
		// Look for the "Host" header in the request
		String[] lines = httpRequest.split("\n");
		for (String line : lines) {
			if (line.toLowerCase().startsWith("host")) {
				return line.split(":")[1].trim(); // Return the value after "Host:"
			}
		}
		return null; // Return null if Host is not found
	}

	/**
	 * Sets up the click listeners and other interactions for the UI buttons.
	 */
	private void setupButtons() {
		button.setOnClickListener(v -> sendRequest(requestTextView.getText().toString()));

		forward.setOnClickListener(v -> goToNextMatch(responseView.getText().toString()));
		backward.setOnClickListener(v -> goToPreviousMatch(responseView.getText().toString()));

		repeaterSearchText.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				searchInFullText(repeaterSearchText.getText().toString(), fullresponse);
				return true;
			}
			return false;
		});

		expand.setOnClickListener(v -> {
			if (frameLayout.getVisibility() == View.GONE)
				frameLayout.setVisibility(View.VISIBLE);
			else {
				frameLayout.setVisibility(View.GONE);
			}
		});
	}

	/**
	 * Replays a given HTTP request to the specified host.
	 *
	 * @param request The HttpRequest object to replay.
	 * @param host    The hostname to send the request to.
	 */
	public void replayRequest(HttpRequest request, String host) {

		try {
			// Create an SSL context for secure communication (TLS 1.2). more configuration needed
			sslContext = SslContextBuilder.forClient().sslProvider(SslProvider.JDK).protocols("TLSv1.2") // Explicitly use TLS 1.2
					.ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE) // Use default cipher suites
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// Bootstrap configuration for Netty client
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup());
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, 443)); // Add SSL handler
				ch.pipeline().addLast(new HttpClientCodec()); // HTTP codec for request/response parsing
				ch.pipeline().addLast(new HttpObjectAggregator(1048576));
				ch.pipeline().addLast(new SimpleChannelInboundHandler<HttpObject>() {
					StringBuilder fullResponse = new StringBuilder();
					BodyType bodyType = null;

					@Override
					protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
						if (msg instanceof HttpResponse) {
							HttpResponse response = (HttpResponse) msg;
							bodyType=handleHttpResponse(response, fullResponse, bodyType);
						}
						List<ByteBuffer> byteBuffers = new ArrayList<>();
						if (msg instanceof HttpContent) {
							ByteBuf content = ((HttpContent) msg).content();
							if (content.readableBytes() > 0) {
								byteBuffers.add(content.nioBuffer());
								if (contentEncoding.isEmpty()) {
									// If no content encoding, append directly
									fullResponse.append(content.toString(CharsetUtil.UTF_8));
								}
							}
							if (msg instanceof LastHttpContent) {
								InputStream stream = getDecodedInputStream(new ByteBufInputStream(content),
										contentEncoding);
								// Append body when it's the last content chunk
								String text = Body.readAll(new InputStreamReader(stream, CharsetUtil.UTF_8));
								if (bodyType != null) {
									for (Beautifier beautifier : beautifiers) {
										if (beautifier.accept(bodyType)) {
											text = beautifier.beautify(text, CharsetUtil.UTF_8);
											break;
										}
									}
								}
								fullResponse.append(text);
								fullresponse = fullResponse.toString();
								startLazyLoad(fullResponse.toString());

							}
						}
					}
				});
			}
		});

		// Connect to the target server and send the request
		bootstrap.connect(host, 443).addListener(new GenericFutureListener<ChannelFuture>() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					Channel channel = future.channel();
					//	HttpRequest request = httpRequest();
					channel.writeAndFlush(request).addListener(f -> {
						if (!f.isSuccess()) {
							showToast("Failed to send request");
						}
					});
				} else {
					showToast("Failed to connect: " + future.cause().getMessage());
				}
			}
		});
	}

	/**
	* Handles an HTTP response, extracting relevant information and appending it to a StringBuilder.
	*
	* @param response      The HTTP response object.
	* @param stringBuilder The StringBuilder to which the response information is appended.
	* @param bodyType      The BodyType (passed by reference and potentially modified).
	*                      It's important that bodyType is initialized before calling this method.
	*/
	private BodyType handleHttpResponse(HttpResponse response, StringBuilder stringBuilder, BodyType bodyType) {
		contentEncoding = response.headers().get(CONTENT_ENCODING_HEADER, "");
		String contentType = response.headers().get(CONTENT_TYPE_HEADER);
		if (contentType != null) {
			ContentType parsed = ContentType.parse(contentType);
			bodyType = Body.getHttpBodyType(parsed);
		}
		stringBuilder.append(String.format("%s %s\n", response.protocolVersion(), response.status()));
		response.headers()
				.forEach(entry -> stringBuilder.append(String.format("%s: %s\n", entry.getKey(), entry.getValue())));
		return bodyType;
	}

	/**
	* Converts a Netty {@link HttpResponse} object to a custom {@link Http1ResponseHeaders} object.
	* This method extracts the status line and headers from the Netty response and creates
	* a new {@link Http1ResponseHeaders} instance containing this information.
	*
	* @param response The Netty {@link HttpResponse} object to convert.
	* @return An {@link Http1ResponseHeaders} object representing the converted headers
	*         and status line from the Netty response.
	*/
	private static Http1ResponseHeaders convertHeader(HttpResponse response) {
		StatusLine statusLine = new StatusLine(response.protocolVersion().text(), response.status().code(),
				response.status().reasonPhrase());
		List<Header> headers = new ArrayList<>();
		response.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
		return new Http1ResponseHeaders(statusLine, headers);
	}

	/*
	* Constructs a {@link FullHttpRequest} from a raw input string.
	 *
	 * @param rawInput The raw input string representing the HTTP request.  This string
	 *                 should be in the format:
	 *                 ```
	 *                 [Request Line]\r\n
	 *                 [Header1]: [Value1]\r\n
	 *                 [Header2]: [Value2]\r\n
	 *                 ...\r\n
	 *                 \r\n  (Empty line separating headers from body)
	 *                 [Body]
	 *                 ```
	 *                 or
	 *                 ```
	 *                 [Request Line]\n
	 *                 [Header1]: [Value1]\n
	 *                 [Header2]: [Value2]\n
	 *                 ...\n
	 *                 \n  (Empty line separating headers from body)
	 *                 [Body]
	 *                 ```
	 *                 Supports both \r\n and \n line endings.
	 *
	 * @return A {@link FullHttpRequest} object representing the parsed request.
	 * @throws IllegalArgumentException If the input is invalid or malformed.
	 */
	public FullHttpRequest constructRequest(String rawInput) {
		if (rawInput == null || rawInput.trim().isEmpty()) {
			throw new IllegalArgumentException("Input is null or empty");
		}

		// Split the raw input into lines
		String[] lines = rawInput.split("\\r?\\n");
		if (lines.length == 0) {
			throw new IllegalArgumentException("Input is empty");
		}

		// Parse the request line (first line)
		String[] requestParts = parseRequestLine(lines[0]);
		String method = requestParts[0];
		String uri = requestParts[1];
		String version = requestParts[2];

		// Extract headers
		Map<String, String> headers = parseHeaders(lines);

		// Extract body
		String body = extractBody(lines);

		// Create ByteBuf for the body content
		ByteBuf content = Unpooled.copiedBuffer(body, StandardCharsets.UTF_8);

		// Construct the FullHttpRequest
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.valueOf(version), HttpMethod.valueOf(method),
				uri, content);

		// Add headers to request
		headers.forEach(request.headers()::set);

		// Set Content-Length header if body is present
		setContentLengthHeader(request, content, body);

		return request;
	}

	/**
	 * Parses the request line (first line of the input).
	 *
	 * @param requestLine The request line string (e.g., "GET /path HTTP/1.1").
	 * @return An array of strings containing the method, URI, and HTTP version.
	 * @throws IllegalArgumentException If the request line is invalid.
	 */
	private String[] parseRequestLine(String requestLine) {
		String[] parts = requestLine.split(" ");
		if (parts.length != 3 || !parts[2].startsWith("HTTP/")) {
			throw new IllegalArgumentException("Invalid request line: " + requestLine);
		}
		return parts;
	}

	/**
	 * Parses the headers from the input lines.
	 *
	 * @param lines An array of strings representing the input lines.
	 * @return A map of header names to header values.
	 * @throws IllegalArgumentException If a header line is invalid.
	 */
	private Map<String, String> parseHeaders(String[] lines) {
		Map<String, String> headers = new LinkedHashMap<>();
		int i = 1;
		while (i < lines.length) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				break; // End of headers
			}

			String[] headerParts = line.split(":", 2);
			if (headerParts.length == 2) {
				headers.put(headerParts[0].trim(), headerParts[1].trim());
			} else {
				throw new IllegalArgumentException("Invalid header format: " + line);
			}
			i++;
		}
		return headers;
	}

	/**
	 * Extracts the body from the input lines.  Finds the first blank line
	 * and everything after it is considered the body.
	 *
	 * @param lines An array of strings representing the input lines.
	 * @return The request body as a string.  Returns an empty string if no body is found.
	 */
	private String extractBody(String[] lines) {
		StringBuilder bodyBuilder = new StringBuilder();
		boolean bodyStarted = false;
		for (int i = lines.length - 1; i >= 0; i--) {
			if (lines[i].trim().isEmpty()) {
				bodyStarted = true;
			} else if (bodyStarted) {
				bodyBuilder.insert(0, lines[i] + "\n");
			}
		}
		return bodyBuilder.toString().trim();
	}

	/**
	 * Sets the Content-Length header in the request.  Handles cases where the
	 * Content-Type indicates encoded content (in which case the readableBytes
	 * of the ByteBuf should be used) and provides a fallback for other content types.
	 * It also ensures that if the content-length is present, it will not be overriden.
	 *
	 * @param request The {@link FullHttpRequest} object.
	 * @param content The {@link ByteBuf} containing the body content.
	 * @param body    The body content as a String.
	 */
	private void setContentLengthHeader(FullHttpRequest request, ByteBuf content, String body) {
		// If Content-Type is encoded, use content's readableBytes
		String contentType = request.headers().get("Content-Type");
		if (contentType != null && contentType.contains("encoded")) {
			request.headers().set("Content-Length", content.readableBytes());
		} else {
			// Default content length based on body length
			request.headers().set("Content-Length", body.length());
		}

		// If Content-Length is already present, do not override it
		if (request.headers().contains("Content-Length")) {
			String length = request.headers().get("Content-Length");
			if (length.isEmpty()) {
				throw new IllegalStateException("Invalid Content-Length header");
			}
		}
	}

	/**
	 * Extracts the body from a text block delimited by double newlines.
	 *
	 * @param text The input text containing the request and body separated by "\n\n".
	 * @return The extracted body.
	 */
	private String extractBody(String text) {
		String[] parts = text.split("\n\n", 2);
		String extractedRequest = parts.length > 0 ? parts[0] : "";
		String extractedBody = parts.length > 1 ? parts[1] : "";
		return extractedBody;
	}

	/**
	 * Retrieves the body content from a {@link Body} object.  Handles both
	 * text and binary body types.
	 *
	 * @param body The {@link Body} object.
	 * @return The body content as a string.
	 */
	private String getBody(Body body) {

		String text = "";
		BodyType bodyType = body.type();
		Charset charset = CharsetUtil.UTF_8;
		if (bodyType.isText()) {

			try (InputStream input = body.getDecodedInputStream();
					Reader reader = new InputStreamReader(input, charset)) {
				text = Body.readAll(reader);
			} catch (IOException e) {

			}
		}
		if (text.equals("")) {
			text = body.content().toString(StandardCharsets.UTF_8);
		}
		return text;
	}

	/**
	* Decodes an input stream based on the specified `contentEncoding`.
	*
	* This method takes a raw input stream and a `contentEncoding` string as input.
	* It attempts to decode the stream based on the encoding. Supported encodings include
	* gzip, deflate, br (Brotli), and lzma. If no encoding is specified or an unsupported
	* encoding is provided, the original input stream is returned.  If an exception occurs
	* during decoding, the original stream is also returned, and an error message is printed.
	*
	* <p><b>Important:</b> Brotli and LZMA decoding require external libraries.  Ensure that
	* the necessary dependencies (e.g., `org.brotli:brotli`, `org.apache.commons.compress:commons-compress` for LZMA)
	* are included in your project if you intend to use these encodings.
	*
	* @param rawInput      The raw input stream.
	* @param contentEncoding The content encoding string (e.g., "gzip", "deflate", "br", "lzma").
	*                      Can be null or empty.
	* @return The decoded input stream, or the original `rawInput` if no decoding is needed,
	*         the encoding is unsupported, or an error occurs during decoding.
	*/
	public InputStream getDecodedInputStream(InputStream rawInput, String contentEncoding) {
		if (rawInput == null || contentEncoding == null || contentEncoding.isEmpty()) {
			return rawInput; // No encoding specified or no content
		}

		InputStream input = rawInput;
		String ce = contentEncoding.toLowerCase();

		try {
			switch (ce) {
			case "identity":
			case "":
				break;
			case "gzip":
				input = new GZIPInputStream(input);
				break;
			case "deflate":
				input = new InflaterInputStream(input, new Inflater(true)); // Handle deflate
				break;
			case "br":
				// Brotli decoding (requires Brotli library)
				input = new BrotliInputStream(input);
				break;
			case "lzma":
				// LZMA decoding (requires xz or SevenZipJBinding library)
				input = new LZMAInputStream(input, -1);
				break;
			default:

				System.err.println("Unsupported content-encoding: " + contentEncoding);
				break;
			}
		} catch (Exception e) {
			System.err.println("Failed to decode stream. Content-Encoding: " + contentEncoding);
			e.printStackTrace();
			// Return the original input stream if decoding fails
			return rawInput;
		}

		return input;
	}

	/**
	 * Searches for the given term in the full text and highlights the matches.
	 *
	 * This method clears any previous matches, finds all occurrences of the search term,
	 * stores their positions, and highlights the first match if any are found.
	 *
	 * @param searchTerm The term to search for.
	 * @param fullText   The full text to search in.
	 */
	private void searchInFullText(String searchTerm, String fullText) {
		matchPositions.clear(); // Clear previous matches
		currentMatchIndex = -1; // Reset the current match index

		// Find all matches and store their positions
		int index = fullText.indexOf(searchTerm);
		while (index != -1) {
			matchPositions.add(new Pair<>(index, index + searchTerm.length()));
			index = fullText.indexOf(searchTerm, index + searchTerm.length());
		}

		// Highlight the first match if any matches are found
		if (!matchPositions.isEmpty()) {
			currentMatchIndex = 0;
			highlightMatch(currentMatchIndex, fullText);
		}
		if (matchPositions.isEmpty()) {
			showToast("No matches");
		}

	}

	/**
	 * Navigates to the next match in the text.
	 *
	 * If there are no matches, this method does nothing. Otherwise, it increments the
	 * current match index (wrapping around to the first match if at the end) and loads
	 * the chunk containing the new match.
	 *
	 * @param fullText The full text.
	 */
	private void goToNextMatch(String fullText) {
		if (matchPositions.isEmpty())
			return;

		currentMatchIndex++;

		// Wrap around to the first match if at the end
		if (currentMatchIndex >= matchPositions.size()) {
			currentMatchIndex = 0;
		}

		// Load the chunk containing the match if it's not already loaded
		loadChunkForMatch(currentMatchIndex, fullText);
	}

	/**
	 * Navigates to the previous match in the text.
	 *
	 * If there are no matches, this method does nothing. Otherwise, it decrements the
	 * current match index (wrapping around to the last match if at the beginning) and
	 * loads the chunk containing the new match.
	 *
	 * @param fullText The full text.
	 */
	private void goToPreviousMatch(String fullText) {
		if (matchPositions.isEmpty())
			return;
		currentMatchIndex--;

		// Wrap around to the last match if at the beginning
		if (currentMatchIndex < 0) {
			currentMatchIndex = matchPositions.size() - 1;
		}
		// Load the chunk containing the match if it's not already loaded
		loadChunkForMatch(currentMatchIndex, fullText);
	}

	/**
	 * Loads the chunk containing the specified match and highlights the match.
	 *
	 * This method calculates the chunk index for the given match, loads the chunk if it's
	 * not already loaded, and then highlights the match within the loaded chunk.
	 *
	 * @param matchIndex The index of the match in the `matchPositions` list.
	 * @param fullText   The full text.
	 */
	private void loadChunkForMatch(int matchIndex, String fullText) {
		if (matchIndex < 0 || matchIndex >= matchPositions.size())
			return; // Invalid index

		// Get the start and end indices of the match
		int start = matchPositions.get(matchIndex).first;
		int end = matchPositions.get(matchIndex).second;

		// Calculate the chunk index for the match
		int chunkIndex = start / DEFAULT_CHUNK_SIZE;

		// Load the chunk if it's not already loaded
		if (chunkIndex != currentChunkIndex) {
			currentChunkIndex = chunkIndex;
			responseView.setText(""); // Clear the text view
			loadNextChunk(); // Load the chunk containing the match
		}

		// Highlight the match
		highlightMatch(matchIndex, fullText);
	}

	/**
	 * Highlights the specified match in the TextView.
	 *
	 * This method uses a `SpannableStringBuilder` to highlight the match with a background color.
	 * It then updates the TextView with the highlighted text and scrolls to the match.
	 *
	 * @param matchIndex The index of the match in the `matchPositions` list.
	 * @param fullText   The full text.
	 */
	private void highlightMatch(int matchIndex, String fullText) {
		if (matchIndex < 0 || matchIndex >= matchPositions.size())
			return; // Invalid index

		// Get the start and end indices of the match
		int start = matchPositions.get(matchIndex).first;
		int end = matchPositions.get(matchIndex).second;

		// Create a SpannableStringBuilder for the full text
		SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(fullText);

		// Highlight the current match
		spannableBuilder.setSpan(new BackgroundColorSpan(getResources().getColor(android.R.color.holo_orange_light)),
				start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		// Update the TextView
		responseView.setText(spannableBuilder);

		// Scroll to the match
		scrollToMatch(start);
	}

	/**
	 * Scrolls the ScrollView to the specified offset, attempting to position the match in view.
	 *
	 * @param matchStart The starting position of the match in the full text.
	 */
	private void scrollToMatch(int matchStart) {
		Layout layout = responseView.getLayout();
		if (layout != null) {
			int line = layout.getLineForOffset(matchStart);
			int y = layout.getLineTop(line);

			// Add an offset to scroll slightly before the match
			int scrollOffset = Math.max(y - 200, 0); // Ensure we don't scroll past the top
			scrollView.post(() -> scrollView.smoothScrollTo(0, scrollOffset));
		}
	}

	/**
	 * Starts the lazy loading process for the given full response text.
	 *
	 * This method splits the response into chunks on a background thread and then loads the
	 * chunks into the TextView as the user scrolls.
	 *
	 * @param fullResponse The full response text.
	 */
	public void startLazyLoad(String fullResponse) {
		if (fullResponse == null || fullResponse.isEmpty()) {
			responseView.setText(""); // Clear the text view
			return;
		}

		// Split the response into chunks on a background thread
		new Thread(() -> {
			List<String> chunks = splitIntoChunks(fullResponse, DEFAULT_CHUNK_SIZE);

			// Update UI on the main thread
			runOnUiThread(() -> {
				textChunks = chunks;
				currentChunkIndex = 0;
				responseView.setText(""); // Clear the text view

				loadNextChunk(); // Load the first chunk

				// Set scroll listener
				scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
					if (scrollView.getChildAt(0).getBottom() <= (scrollView.getHeight() + scrollView.getScrollY())) {
						// User reached the bottom
						loadNextChunk();
					}
				});
			});
		}).start();
	}

	/**
	 * Splits the given text into chunks of the specified size.
	 *
	 * @param text      The text to split.
	 * @param chunkSize The size of each chunk.
	 * @return A list of text chunks.
	 */
	private List<String> splitIntoChunks(String text, int chunkSize) {
		List<String> chunks = new ArrayList<>();
		if (text == null || text.isEmpty() || chunkSize <= 0) {
			return chunks;
		}

		int length = text.length();
		for (int i = 0; i < length; i += chunkSize) {
			chunks.add(text.substring(i, Math.min(length, i + chunkSize)));
		}
		return chunks;
	}

	/**
	 * Helper method to run code on the UI thread.
	 *
	 * @param action The `Runnable` to execute on the UI thread.
	 */

	private void runOnUiThread(Runnable action) {
		new Handler(Looper.getMainLooper()).post(action);
	}

	/**
	 * Loads and appends the next chunk of text to the TextView.
	 * Highlights any matches found within the chunk.
	 */
	private void loadNextChunk() {
		if (currentChunkIndex < textChunks.size()) {
			String chunk = textChunks.get(currentChunkIndex);
			SpannableStringBuilder highlightedChunk = new SpannableStringBuilder(chunk);

			// Check if any matches fall within this chunk
			int chunkStart = currentChunkIndex * DEFAULT_CHUNK_SIZE;
			int chunkEnd = chunkStart + chunk.length();

			for (Pair<Integer, Integer> match : matchPositions) {
				int matchStart = match.first;
				int matchEnd = match.second;

				if (matchStart >= chunkStart && matchEnd <= chunkEnd) {
					// Adjust match positions relative to the chunk
					int relativeStart = matchStart - chunkStart;
					int relativeEnd = matchEnd - chunkStart;

					// Highlight the match
					highlightedChunk.setSpan(
							new BackgroundColorSpan(
									ContextCompat.getColor(getContext(), android.R.color.holo_orange_light)),
							relativeStart, relativeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}

			// Append the chunk to the TextView
			responseView.append(highlightedChunk);
			currentChunkIndex++;
		}
	}

	/**
	 * Highlights and formats the given HTTP request text.
	 *
	 * This method parses the HTTP request text, identifies header names and values,
	 * and applies different foreground colors to them.  It also handles HTTP/2
	 * pseudo-headers and the HTTP version line.
	 *
	 * @param requestEditText The raw HTTP request text.
	 * @return A `SpannableStringBuilder` containing the formatted and highlighted text.
	 */
	private SpannableStringBuilder highlightAndFormatHttpRequest(String requestEditText) {

		StringBuilder formattedText = new StringBuilder();
		String[] lines = requestEditText.split("\n");

		for (String line : lines) {
			if (line.contains(":")) {
				int colonIndex = line.indexOf(":");
				colonIndex = colonIndex != -1 && line.indexOf(":", colonIndex + 1) != -1
						? line.indexOf(":", colonIndex + 1)
						: colonIndex;

				String headerName = line.substring(0, colonIndex).trim();
				String headerValue = line.substring(colonIndex + 1).trim();

				// For HTTP/2 pseudo-headers, maintain a different format if needed
				if (headerName.startsWith(":")) {
					formattedText.append(headerName.trim()).append(":  ").append(headerValue.trim()).append("\n");
				} else {
					formattedText.append(headerName.trim()).append(":  ").append(headerValue.trim()).append("\n");
				}
			} else {
				// Keep non-header lines (like HTTP method and path) unchanged
				formattedText.append(line).append("\n");
			}
		}

		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(formattedText);
		lines = formattedText.toString().split("\n");

		int start = 0; // Track the cumulative position in the text
		for (String line : lines) {
			if (line.contains("\n\n")) {
				return stringBuilder;
			}
			if (line.contains(":")) {

				int colonIndex = line.indexOf(":");
				colonIndex = colonIndex != -1 && line.indexOf(":", colonIndex + 1) != -1
						? line.indexOf(":", colonIndex + 1)
						: colonIndex;
				// Highlight the header name (before the colon)
				stringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#efb92c")), start, start + colonIndex,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				// Optionally, highlight pseudo-headers differently

			} else if (line.startsWith("HTTP")) {

				stringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#2596be")), 0, 8,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			}
			// Move the start position to the next line
			start += line.length() + 1; // +1 for the newline character
		}
		return stringBuilder;
	}

}
