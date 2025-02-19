package com.proxy.data;




import com.proxy.store.Body;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;

public abstract class HttpHeaders implements Serializable {
    private static final long serialVersionUID = 9092421542605849007L;
    private final List<Header> headers;

    protected HttpHeaders(List<Header> headers) {
        this.headers = Objects.requireNonNull(headers);
    }

    public Optional<String> getHeader(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(Header::value)
                .findFirst();
    }

    
    public List<String> getHeaders(String name) {
        return headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(Header::value)
                .collect(toList());
				
    }

    /**
     * Get content type from http header
     */
    public Optional<ContentType> contentType() {
        return getHeader("Content-type").map(ContentType::parse);
    }

    public Optional<String> contentEncoding() {
        return getHeader("Content-Encoding");
    }

    public List<Header> headers() {
        return headers;
    }

    public Body createBody() {
		
        return Body.create(contentType().orElse(ContentType.binary), contentEncoding().orElse(""));
    }

    /**
     * Convert to raw lines string
     */
    public abstract List<String> rawLines();

    public abstract List<NameValue> cookieValues();
}