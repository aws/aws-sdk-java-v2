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

package software.amazon.awssdk.core.http;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Represents an HTTP response returned by an AWS service in response to a
 * service request.
 */
@SdkProtectedApi
@ReviewBeforeRelease("Make sure we aren't exposing this anywhere")
public final class HttpResponse implements Abortable {

    private final SdkHttpFullRequest request;
    private final Abortable abortable;

    private String statusText;
    private int statusCode;
    private InputStream content;
    private Map<String, String> headers = new HashMap<>();

    /**
     * Constructs a new HttpResponse associated with the specified request.
     *
     * @param request The associated request that generated this response.
     */
    public HttpResponse(SdkHttpFullRequest request) {
        this(request, null);
    }

    public HttpResponse(SdkHttpFullRequest request, Abortable abortable) {
        this.request = request;
        this.abortable = abortable;
    }

    /**
     * Returns the original request associated with this response.
     *
     * @return The original request associated with this response.
     */
    public SdkHttpFullRequest getRequest() {
        return request;
    }

    /**
     * Returns the HTTP headers returned with this response.
     *
     * @return The set of HTTP headers returned with this HTTP response.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Looks up a header by name and returns it's value. Does case insensitive comparison.
     *
     * @param headerName Name of header to get value for.
     * @return The header value of the given header. Null if header is not present.
     */
    public String getHeader(String headerName) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase(headerName)) {
                return header.getValue();
            }
        }
        return null;
    }

    /**
     * Adds an HTTP header to the set associated with this response.
     *
     * @param name  The name of the HTTP header.
     * @param value The value of the HTTP header.
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Returns the input stream containing the response content.
     *
     * @return The input stream containing the response content.
     */
    public InputStream getContent() {
        return content;
    }

    /**
     * Sets the input stream containing the response content.
     *
     * @param content The input stream containing the response content.
     */
    public void setContent(InputStream content) {
        this.content = content;
    }

    /**
     * Returns the HTTP status text associated with this response.
     *
     * @return The HTTP status text associated with this response.
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * Sets the HTTP status text returned with this response.
     *
     * @param statusText The HTTP status text (ex: "Not found") returned with this
     *                   response.
     */
    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    /**
     * Returns the HTTP status code (ex: 200, 404, etc) associated with this
     * response.
     *
     * @return The HTTP status code associated with this response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code that was returned with this response.
     *
     * @param statusCode The HTTP status code (ex: 200, 404, etc) associated with this
     *                   response.
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * If we get back any 2xx status code, then we know we should treat the service call as successful.
     */
    public boolean isSuccessful() {
        return HttpStatusFamily.of(statusCode) == HttpStatusFamily.SUCCESSFUL;
    }

    @Override
    public void abort() {
        if (abortable != null) {
            abortable.abort();
        }
    }
}
