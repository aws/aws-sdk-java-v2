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
 * <p>Three factory methods are provided:
 * <ul>
 *   <li>{@link #fromString(String)} — parses a URL string using simple string operations (no URI construction)</li>
 *   <li>{@link #fromComponents(String, String, int, String)} — creates from individual components (no query/fragment)</li>
 *   <li>{@link #fromUri(URI)} — creates from an existing URI (pre-populates the cached URI field, preserves
 *       query and fragment)</li>
 * </ul>
 */
@SdkPublicApi
public final class EndpointUrl {

    private final String scheme;
    private final String host;
    private final int port;
    private final String encodedPath;
    private final String queryAndFragment;
    private final String rawUrl;

    // Inline lazy URI — avoids dependency on utils module's Lazy<T>.
    // Uses double-checked locking.
    private volatile URI uri;

    private EndpointUrl(String scheme, String host, int port, String encodedPath,
                        String queryAndFragment, String rawUrl, URI uri) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.encodedPath = encodedPath;
        this.queryAndFragment = queryAndFragment;
        this.rawUrl = rawUrl;
        this.uri = uri;
    }

    /**
     * Parse a URL string into its components without constructing a {@link URI}.
     *
     * <p>Performs minimal string splitting only — no validation beyond checking for the {@code ://} separator.
     * The original URL string is retained for faithful URI reconstruction via {@link #toUri()}. Path, query and fragment
     * components (if present) MUST already be url encoded.
     *
     * <p>Expected format: {@code scheme://host[:port][/encodedPath][?query][#fragment]}
     *
     * @param url the URL string to parse
     * @return a new {@code EndpointUrl} with pre-parsed components
     * @throws IllegalArgumentException if the URL does not contain {@code ://}
     */
    public static EndpointUrl fromString(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            throw new IllegalArgumentException("Invalid URL: missing '://' separator: " + url);
        }

        String scheme = url.substring(0, schemeEnd);
        int authorityStart = schemeEnd + 3;

        // Find where authority ends: first '/', '?', or '#' after authority start, or end of string.
        // RFC 3986: authority is terminated by '/', '?', '#', or end of URI.
        int pathStart = -1;
        int len = url.length();
        for (int i = authorityStart; i < len; i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                pathStart = i;
                break;
            }
        }

        String authority;
        String pathAndRest;
        if (pathStart < 0) {
            authority = url.substring(authorityStart);
            pathAndRest = "";
        } else {
            authority = url.substring(authorityStart, pathStart);
            pathAndRest = url.substring(pathStart);
        }

        // Separate path from query/fragment
        String encodedPath;
        String queryAndFragment;
        int queryStart = pathAndRest.indexOf('?');
        int fragmentStart = pathAndRest.indexOf('#');
        int separatorPos = -1;
        if (queryStart >= 0 && fragmentStart >= 0) {
            separatorPos = Math.min(queryStart, fragmentStart);
        } else if (queryStart >= 0) {
            separatorPos = queryStart;
        } else if (fragmentStart >= 0) {
            separatorPos = fragmentStart;
        }

        if (separatorPos >= 0) {
            encodedPath = pathAndRest.substring(0, separatorPos);
            queryAndFragment = pathAndRest.substring(separatorPos);
        } else {
            encodedPath = pathAndRest;
            queryAndFragment = "";
        }

        // Split authority into host and optional port, handling IPv6 addresses
        String host;
        int port;
        if (!authority.isEmpty() && authority.charAt(0) == '[') {
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

        return new EndpointUrl(scheme, host, port, encodedPath, queryAndFragment, url, null);
    }

    /**
     * Create an {@code EndpointUrl} from individual components.
     *
     * <p>Used internally (e.g., by {@code addHostPrefix} and codegen) to avoid re-parsing.
     * The {@code rawUrl} field is {@code null} in this case, so {@link #toUri()} reconstructs
     * the URI from components. No query or fragment is included.
     *
     * @param scheme      the URL scheme (e.g., "https")
     * @param host        the hostname (e.g., "s3.us-east-1.amazonaws.com")
     * @param port        the port number, or -1 if not specified
     * @param encodedPath the encoded path (e.g., "/bucket/key"), or empty string if no path
     * @return a new {@code EndpointUrl}
     */
    public static EndpointUrl fromComponents(String scheme, String host, int port, String encodedPath) {
        return new EndpointUrl(scheme, host, port, encodedPath, "", null, null);
    }

    /**
     * Create an {@code EndpointUrl} from individual components, including query and fragment.
     *
     * <p>This overload is used when reconstructing an EndpointUrl with modifications (e.g., host prefix)
     * while preserving the original query and fragment components.
     *
     * @param scheme            the URL scheme (e.g., "https")
     * @param host              the hostname (e.g., "s3.us-east-1.amazonaws.com")
     * @param port              the port number, or -1 if not specified
     * @param encodedPath       the encoded path (e.g., "/bucket/key"), or empty string if no path
     * @param queryAndFragment  the query and fragment string (e.g., "?key=value#section"), or empty string if none
     * @return a new {@code EndpointUrl}
     */
    public static EndpointUrl fromComponents(String scheme, String host, int port, String encodedPath,
                                             String queryAndFragment) {
        return new EndpointUrl(scheme, host, port, encodedPath, queryAndFragment, null, null);
    }

    /**
     * Create an {@code EndpointUrl} from an existing {@link URI}.
     *
     * <p>The URI field is pre-populated, so {@link #toUri()}
     * returns the original URI instance without any additional construction.
     *
     * @param uri the URI to create from
     * @return a new {@code EndpointUrl} with components extracted from the URI
     */
    public static EndpointUrl fromUri(URI uri) {
        String rawPath = uri.getRawPath();
        String queryAndFragment = buildQueryAndFragment(uri);
        return new EndpointUrl(
            uri.getScheme(),
            uri.getHost(),
            uri.getPort(),
            rawPath != null ? rawPath : "",
            queryAndFragment,
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
     * Returns the query and fragment portion of the URL (e.g., {@code "?key=value#section"}),
     * or an empty string if neither is present.
     */
    public String queryAndFragment() {
        return queryAndFragment;
    }

    /**
     * Returns the {@link URI} representation. Lazily constructed on first call using double-checked locking.
     *
     * <p>When created via {@link #fromString(String)}, uses the original URL string for faithful reconstruction.
     * When created via {@link #fromComponents(String, String, int, String)}  reconstructs from components (no query/fragment).
     * When created via {@link #fromUri(URI)}, returns the original URI instance.
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
                        StringBuilder sb = new StringBuilder();
                        sb.append(scheme).append("://").append(host);
                        if (port >= 0) {
                            sb.append(':').append(port);
                        }
                        sb.append(encodedPath);
                        sb.append(queryAndFragment);
                        result = URI.create(sb.toString());
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
        if (encodedPath != null ? !encodedPath.equals(that.encodedPath) : that.encodedPath != null) {
            return false;
        }
        return queryAndFragment.equals(that.queryAndFragment);
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (encodedPath != null ? encodedPath.hashCode() : 0);
        result = 31 * result + queryAndFragment.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EndpointUrl("
               + "scheme=" + scheme
               + ", host=" + host
               + ", port=" + port
               + ", encodedPath=" + encodedPath
               + ", queryAndFragment=" + queryAndFragment
               + ")";
    }

    /**
     * Build the query and fragment string from a URI, or return empty string if neither is present.
     */
    private static String buildQueryAndFragment(URI uri) {
        String rawQuery = uri.getRawQuery();
        String rawFragment = uri.getRawFragment();
        if (rawQuery == null && rawFragment == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (rawQuery != null) {
            sb.append('?').append(rawQuery);
        }
        if (rawFragment != null) {
            sb.append('#').append(rawFragment);
        }
        return sb.toString();
    }
}
