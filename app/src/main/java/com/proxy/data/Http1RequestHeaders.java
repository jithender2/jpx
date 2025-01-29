package com.proxy.data;

import com.proxy.data.RequestLine;
import com.proxy.utils.NameValues;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.List;
import  static java.util.stream.Collectors.toList;
public class Http1RequestHeaders extends Http1Headers implements HttpRequestHeaders, Serializable {
	private static final long serialVersionUID = 6625148408370480848L;
	private RequestLine requestLine;

	public Http1RequestHeaders(RequestLine requestLine, List<Header> headers) {
		super(headers);
		this.requestLine = requestLine;
	}

	public static Http1RequestHeaders parse(String rawRequestLine, List<String> rawHeaders) {
		return new Http1RequestHeaders(RequestLine.parse(rawRequestLine),
				rawHeaders.stream().map(Header::parse).collect(toList()));
	}

	@Override
	public List<String> rawLines() {
		List<String> rawLines = new ArrayList<>(headers().size() + 1);
		rawLines.add(requestLine.rawRequestLine());
		rawLines.addAll(NameValues.toAlignText(headers(), ": "));
		return rawLines;
	}

	/**
	 * If this request/response has body.
	 */
	public boolean hasBody() {
		return !"TRACE".equalsIgnoreCase(requestLine.method()) && !"GET".equalsIgnoreCase(requestLine.method())
				&& !"OPTIONS".equalsIgnoreCase(requestLine.method());
	}

	@Override
	public List<NameValue> cookieValues() {
		return getHeaders("Cookie").stream().flatMap(v -> Stream.of(v.split(";"))).map(String::trim)
				.map(Parameter::parse).collect(toList());
	}

	public RequestLine requestLine() {
		return requestLine;
	}

	@Override
	public String toString() {
		return "Http1RequestHeaders(requestLine=" + requestLine + ", headers=" + super.toString() + ")";
	}

	@Override
	public String method() {
		return requestLine.method();
	}

	@Override
	public String path() {
		return requestLine.path();
	}

	@Override
	public String version() {
		return requestLine.version();
	}
}