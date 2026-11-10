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
     * <p>Performs minimal string splitting only. The original URL string is retained for faithful URI reconstruction
     * via {@link #toUri()}. Path, query and fragment components (if present) MUST already be url encoded.
     *
     * <p>Expected format: {@code scheme://[userinfo@]host[:port][/encodedPath][?query][#fragment]}
     *
     * <p>Component extraction matches {@link URI} for well-formed URLs:
     * <ul>
     *   <li>Userinfo is excluded from {@link #host()}, matching {@link URI#getHost()}. It remains part of the
     *       string returned by {@link #toUri()}, matching {@link URI#toString()}.</li>
     *   <li>A trailing {@code ':'} with no digits (e.g. {@code https://example.com:}) yields a port of {@code -1},
     *       matching {@link URI#getPort()}.</li>
     *   <li>An IPv6 literal is stored with its enclosing brackets (e.g. {@code [::1]}).</li>
     * </ul>
     *
     * @param url the URL string to parse
     * @return a new {@code EndpointUrl} with pre-parsed components
     * @throws IllegalArgumentException if the URL has no scheme, has a non-numeric or out-of-range port, has more
     *                                  than one {@code ':'} separating host and port, or has an unterminated IPv6
     *                                  literal
     */
    public static EndpointUrl fromString(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            throw invalidUrl(url, "unable to parse a scheme, which is required. Endpoint URLs must be absolute and "
                                  + "begin with a scheme such as 'https://'");
        }

        String scheme = url.substring(0, schemeEnd);
        int authorityStart = schemeEnd + 3;

        // Single pass over the authority. This locates where the authority ends and, at the same time, records the
        // positions needed to split it into [userinfo '@'] host [':' port], so the authority is only scanned once.
        // RFC 3986: the authority is terminated by '/', '?', '#', or the end of the URI.
        int len = url.length();
        int authorityEnd = len;
        int lastAt = -1;
        int lastColon = -1;
        int closeBracket = -1;
        for (int i = authorityStart; i < len; i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                authorityEnd = i;
                break;
            }
            if (c == ':') {
                lastColon = i;
            } else if (c == '@') {
                lastAt = i;
            } else if (c == ']') {
                closeBracket = i;
            }
        }

        String pathAndRest = authorityEnd < len ? url.substring(authorityEnd) : "";

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

        // Userinfo, when present, runs up to the last '@' and is not part of the host.
        int hostStart = lastAt >= 0 ? lastAt + 1 : authorityStart;

        // A ':' only separates host from port when it falls after any userinfo and, for a bracketed IPv6 literal,
        // after the closing ']'. Colons inside userinfo (user:pass@) or inside brackets ([::1]) are not port
        // separators.
        boolean hasPort = lastColon >= hostStart && lastColon > closeBracket;
        int hostEnd = hasPort ? lastColon : authorityEnd;
        String host = url.substring(hostStart, hostEnd);

        if (!host.isEmpty() && host.charAt(0) == '[') {
            if (closeBracket < hostStart || closeBracket >= hostEnd) {
                throw invalidUrl(url, "malformed IPv6 host: missing closing ']'");
            }
        } else if (host.indexOf(':') >= 0) {
            throw invalidUrl(url, "malformed host '" + host + "': expected at most one ':' separating host and port");
        }

        int port = hasPort ? parsePort(url, lastColon + 1, authorityEnd) : -1;

        return new EndpointUrl(scheme, host, port, encodedPath, queryAndFragment, url, null);
    }

    /**
     * Parse the port from {@code url} in the range {@code [start, end)}.
     *
     * <p>An empty range means the URL ended with a bare ':', which {@link URI} treats as no port.
     */
    private static int parsePort(String url, int start, int end) {
        if (start == end) {
            return -1;
        }

        long port = 0;
        for (int i = start; i < end; i++) {
            char c = url.charAt(i);
            if (c < '0' || c > '9') {
                throw invalidUrl(url, "invalid port '" + url.substring(start, end) + "': port must be a number");
            }
            port = port * 10 + (c - '0');
            if (port > Integer.MAX_VALUE) {
                throw invalidUrl(url, "invalid port '" + url.substring(start, end) + "': port is out of range");
            }
        }
        return (int) port;
    }

    private static IllegalArgumentException invalidUrl(String url, String reason) {
        return new IllegalArgumentException("Invalid endpoint URL '" + url + "': " + reason);
    }

    /**
     * Create an {@code EndpointUrl} from individual components.
     *
     * <p>Used internally (e.g., by {@code addHostPrefix} and codegen) to avoid re-parsing.
     * The {@code rawUrl} field is {@code null} in this case, so {@link #toUri()} reconstructs
     * the URI from components. No query or fragment is included.
     *
     * <p><b>Equivalence contract:</b> For any valid endpoint URL string {@code s} with no query or fragment,
     * {@code fromComponents(scheme, host, port, path)} MUST produce an {@code EndpointUrl} that equals
     * {@code fromString(scheme + "://" + host + (port >= 0 ? ":" + port : "") + path)}.
     * The codegen relies on this equivalence — see {@code EndpointUrlCodeEmitter}.
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
