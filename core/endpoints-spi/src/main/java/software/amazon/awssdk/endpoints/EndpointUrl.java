/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.endpoints;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A lightweight, immutable representation of a resolved endpoint URL that stores pre-parsed components
 * (scheme, host, port, path) as strings, avoiding the cost of {@link URI} construction.
 *
 * <p>This type is the core optimization for endpoint resolution performance. Instead of constructing a
 * {@code java.net.URI} on every request (which performs extensive validation and parsing), {@code EndpointUrl}
 * stores the URL components as simple strings and only constructs a {@code URI} lazily when {@link #toUri()}
 * is explicitly called.</p>
 *
 * <p>Three factory methods are provided:</p>
 * <ul>
 *   <li>{@link #parse(String)} — parses a URL string using simple string operations (no URI construction)</li>
 *   <li>{@link #of(String, String, int, String)} — creates from individual components</li>
 *   <li>{@link #fromUri(URI)} — creates from an existing URI (pre-populates the cached URI field)</li>
 * </ul>
 */
@SdkPublicApi
public final class EndpointUrl {

    private final String scheme;
    private final String host;
    private final int port;
    private final String encodedPath;
    private final String rawUrl;

    // Inline lazy URI — avoids dependency on utils module's Lazy<T>.
    // Uses double-checked locking (Requirement 9.1).
    private volatile URI uri;

    private EndpointUrl(String scheme, String host, int port, String encodedPath,
                        String rawUrl, URI uri) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.encodedPath = encodedPath;
        this.rawUrl = rawUrl;
        this.uri = uri;
    }

    /**
     * Parse a URL string into its components without constructing a {@link URI}.
     *
     * <p>Performs minimal string splitting only — no validation beyond checking for the {@code ://} separator.
     * The original URL string is retained for faithful URI reconstruction via {@link #toUri()}.</p>
     *
     * <p>Expected format: {@code scheme://host[:port][/path]}</p>
     *
     * @param url the URL string to parse
     * @return a new {@code EndpointUrl} with pre-parsed components
     * @throws IllegalArgumentException if the URL does not contain {@code ://}
     */
    public static EndpointUrl parse(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            throw new IllegalArgumentException("Invalid URL: missing '://' separator: " + url);
        }

        String scheme = url.substring(0, schemeEnd);
        int authorityStart = schemeEnd + 3;

        // Find where authority ends (first '/' after authority, or end of string)
        int pathStart = url.indexOf('/', authorityStart);

        String authority;
        String encodedPath;
        if (pathStart < 0) {
            authority = url.substring(authorityStart);
            encodedPath = "";
        } else {
            authority = url.substring(authorityStart, pathStart);
            encodedPath = url.substring(pathStart);
        }

        // Split authority into host and optional port, handling IPv6 addresses
        String host;
        int port;
        if (authority.charAt(0) == '[') {
            // IPv6: [::1]:8080
            int bracketEnd = authority.indexOf(']');
            host = authority.substring(0, bracketEnd + 1);
            if (bracketEnd + 1 < authority.length() && authority.charAt(bracketEnd + 1) == ':') {
                port = Integer.parseInt(authority.substring(bracketEnd + 2));
            } else {
                port = -1;
            }
        } else {
            int colonPos = authority.lastIndexOf(':');
            if (colonPos < 0) {
                host = authority;
                port = -1;
            } else {
                host = authority.substring(0, colonPos);
                port = Integer.parseInt(authority.substring(colonPos + 1));
            }
        }

        return new EndpointUrl(scheme, host, port, encodedPath, url, null);
    }

    /**
     * Create an {@code EndpointUrl} from individual components.
     *
     * <p>Used internally (e.g., by {@code addHostPrefix}) to avoid re-parsing. The {@code rawUrl} field
     * is {@code null} in this case, so {@link #toUri()} reconstructs the URI from components.</p>
     *
     * @param scheme      the URL scheme (e.g., "https")
     * @param host        the hostname (e.g., "s3.us-east-1.amazonaws.com")
     * @param port        the port number, or -1 if not specified
     * @param encodedPath the encoded path (e.g., "/bucket/key"), or empty string if no path
     * @return a new {@code EndpointUrl}
     */
    public static EndpointUrl of(String scheme, String host, int port, String encodedPath) {
        return new EndpointUrl(scheme, host, port, encodedPath, null, null);
    }

    /**
     * Create an {@code EndpointUrl} from an existing {@link URI}.
     *
     * <p>This is the backward-compatibility path. The URI field is pre-populated, so {@link #toUri()}
     * returns the original URI instance without any additional construction.</p>
     *
     * @param uri the URI to create from
     * @return a new {@code EndpointUrl} with components extracted from the URI
     */
    public static EndpointUrl fromUri(URI uri) {
        String rawPath = uri.getRawPath();
        return new EndpointUrl(
            uri.getScheme(),
            uri.getHost(),
            uri.getPort(),
            rawPath != null ? rawPath : "",
            null,
            uri
        );
    }

    /**
     * Returns the URL scheme (e.g., "https").
     */
    public String scheme() {
        return scheme;
    }

    /**
     * Returns the hostname (e.g., "s3.us-east-1.amazonaws.com").
     */
    public String host() {
        return host;
    }

    /**
     * Returns the port number, or -1 if not explicitly specified.
     */
    public int port() {
        return port;
    }

    /**
     * Returns the encoded path (e.g., "/bucket/key"), or an empty string if no path is present.
     */
    public String encodedPath() {
        return encodedPath;
    }

    /**
     * Returns the {@link URI} representation. Lazily constructed on first call using double-checked locking.
     *
     * <p>When created via {@link #parse(String)}, uses the original URL string for faithful reconstruction
     * (preserving query params, fragments, etc.). When created via {@link #of(String, String, int, String)},
     * reconstructs from components. When created via {@link #fromUri(URI)}, returns the original URI instance.</p>
     *
     * @return the URI representation of this endpoint URL
     */
    public URI toUri() {
        URI result = uri;
        if (result == null) {
            synchronized (this) {
                result = uri;
                if (result == null) {
                    if (rawUrl != null) {
                        result = URI.create(rawUrl);
                    } else {
                        result = URI.create(scheme + "://" + host
                                            + (port >= 0 ? ":" + port : "")
                                            + encodedPath);
                    }
                    uri = result;
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EndpointUrl that = (EndpointUrl) o;

        if (port != that.port) {
            return false;
        }
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) {
            return false;
        }
        if (host != null ? !host.equals(that.host) : that.host != null) {
            return false;
        }
        return encodedPath != null ? encodedPath.equals(that.encodedPath) : that.encodedPath == null;
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (encodedPath != null ? encodedPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EndpointUrl("
               + "scheme=" + scheme
               + ", host=" + host
               + ", port=" + port
               + ", encodedPath=" + encodedPath
               + ")";
    }
}
