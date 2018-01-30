/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An immutable HTTP request without access to the request body. {@link SdkHttpFullRequest} should be used when access to a
 * request body stream is required.
 */
@SdkPublicApi
@Immutable
public interface SdkHttpRequest extends SdkHttpHeaders {
    /**
     * Returns the protocol that should be used for HTTP communication.
     *
     * <p>This will always be "https" or "http" (lowercase).</p>
     *
     * @return Either "http" or "https" depending on which protocol should be used.
     */
    String protocol();

    /**
     * Returns the host that should be communicated with.
     *
     * <p>This will never be null.</p>
     *
     * @return The host to which the request should be sent.
     */
    String host();

    /**
     * The port that should be used for HTTP communication. If this was not configured when the request was created, it will be
     * derived from the protocol. For "http" it would be 80, and for "https" it would be 443.
     *
     * <p>Important Note: AWS signing DOES NOT include the port when the request is signed if the default port for the protocol is
     * being used. When sending requests via http over port 80 or via https over port 443, the URI or host header MUST NOT include
     * the port or a signature error will be raised from the service for signed requests. HTTP plugin implementers are encouraged
     * to use the {@link #getUri()} method for generating the URI to use for communicating with AWS to ensure the URI used in the
     * request matches the URI used during signing.</p>
     *
     * @return The port that should be used for HTTP communication.
     */
    int port();

    /**
     * Returns the URL-encoded path that should be used in the HTTP request.
     *
     * <p>If a path is configured, the path will always start with '/' and may or may not end with '/', depending on what the
     * service might expect. If a path is not configured, this will always return empty-string (ie. ""). Note that '/' is also a
     * valid path.</p>
     *
     * @return The path to the resource being requested.
     */
    String encodedPath();

    /**
     * Returns a map of all non-URL encoded parameters in this request. HTTP plugins can use
     * {@link SdkHttpUtils#encodeQueryParameters(Map)} to encode parameters into map-form, or
     * {@link SdkHttpUtils#encodeAndFlattenQueryParameters(Map)} to encode the parameters into uri-formatted string form.
     *
     * <p>This will never be null. If there are no parameters an empty map is returned.</p>
     *
     * @return An unmodifiable map of all non-encoded parameters in this request.
     */
    Map<String, List<String>> rawQueryParameters();

    /**
     * Convert this HTTP request's protocol, host, port, path and query string into a properly-encoded URI string that matches the
     * URI string used for AWS request signing.
     *
     * <p>The URI's port will be missing (-1) when the {@link #port()} is the default port for the {@link #protocol()}. (80 for
     * http and 443 for https). This is to reflect the fact that request signature does not include the port.</p>
     *
     * @return The URI for this request, formatted in the same way the AWS HTTP request signer uses the URI in the signature.
     */
    default URI getUri() {
        // We can't create a URI by simply passing the query parameters into the URI constructor that takes a query string,
        // because URI will re-encode them. Because we want to encode them using our encoder, we have to build the URI
        // ourselves and pass it to the single-argument URI constructor that doesn't perform the encoding.

        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(rawQueryParameters())
                                                .map(value -> "?" + value)
                                                .orElse("");

        // Do not include the port in the URI when using the default port for the protocol.
        String portString = SdkHttpUtils.isUsingStandardPort(protocol(), port()) ? "" : ":" + port();

        return URI.create(protocol() + "://" + host() + portString + encodedPath() + encodedQueryString);
    }

    /**
     * Returns the HTTP method (GET, POST, etc) to use when sending this request.
     *
     * <p>This will never be null.</p>
     *
     * @return The HTTP method to use when sending this request.
     */
    SdkHttpMethod method();
}
