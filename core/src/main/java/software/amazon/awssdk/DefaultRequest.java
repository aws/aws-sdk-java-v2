/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.event.ProgressInputStream;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.util.json.Jackson;

/**
 * Default implementation of the {@linkplain Request} interface.
 * <p>
 * This class is only intended for internal use inside the AWS client libraries.
 * Callers shouldn't ever interact directly with objects of this class.
 */
@NotThreadSafe
public class DefaultRequest<T> implements Request<T> {

    /**
     * The original, user facing request object which this internal request
     * object is representing
     */
    private final AmazonWebServiceRequest originalRequest;

    /** The resource path being requested. */
    private String resourcePath;
    /**
     * Map of the parameters being sent as part of this request.
     * <p>
     * Note that a LinkedHashMap is used, since we want to preserve the
     * insertion order so that members of a list parameter will still be ordered
     * by their indices when they are marshalled into the query string.
     *
     * Lists values in this Map must use an implementation that allows
     * null values to be present.
     */
    private Map<String, List<String>> parameters = new LinkedHashMap<String, List<String>>();
    /** Map of the headers included in this request. */
    private Map<String, String> headers = new HashMap<String, String>();
    /** The service endpoint to which this request should be sent. */
    private URI endpoint;
    /** The name of the service to which this request is being sent. */
    private String serviceName;
    /** The HTTP method to use when sending this request. */
    private HttpMethodName httpMethod = HttpMethodName.POST;
    /** An optional stream from which to read the request payload. */
    private InputStream content;
    /** An optional time offset to account for clock skew. */
    private int timeOffset;
    /** All AWS Request metrics are collected into this object. */
    private AwsRequestMetrics metrics;

    /**
     * Constructs a new DefaultRequest with the specified service name and the
     * original, user facing request object.
     *
     * @param serviceName
     *            The name of the service to which this request is being sent.
     * @param originalRequest
     *            The original, user facing, AWS request being represented by
     *            this internal request object.
     */
    public DefaultRequest(AmazonWebServiceRequest originalRequest, String serviceName) {
        this.serviceName = serviceName;
        this.originalRequest = originalRequest == null
                               ? AmazonWebServiceRequest.NOOP
                               : originalRequest;
    }

    /**
     * Constructs a new DefaultRequest with the specified service name and no
     * specified original, user facing request object.
     *
     * @param serviceName
     *            The name of the service to which this request is being sent.
     */
    public DefaultRequest(String serviceName) {
        this(null, serviceName);
    }


    /**
     * Returns the original, user facing request object which this internal
     * request object is representing.
     *
     * @return The original, user facing request object which this request
     *         object is representing.
     */
    public AmazonWebServiceRequest getOriginalRequest() {
        return originalRequest;
    }

    /**
     * @see Request#addHeader(java.lang.String, java.lang.String)
     */
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * @see Request#getHeaders()
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @see Request#setHeaders(java.util.Map)
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    /**
     * @see Request#getResourcePath()
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * @see Request#setResourcePath(java.lang.String)
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * @see Request#addParameter(java.lang.String, java.lang.String)
     */
    public void addParameter(String name, String value) {
        List<String> paramList = parameters.get(name);
        if (paramList == null) {
            paramList = new ArrayList<String>();
            parameters.put(name, paramList);
        }
        paramList.add(value);
    }

    /**
     *
     * @see Request#addParameters(java.lang.String, java.util.List)
     */
    public void addParameters(String name, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            addParameter(name, value);
        }
    }

    /**
     * @see Request#getParameters()
     */
    public Map<String, List<String>> getParameters() {
        return parameters;
    }

    /**
     * @see Request#setParameters(java.util.Map)
     */
    public void setParameters(Map<String, List<String>> parameters) {
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }

    /**
     * @see Request#withParameter(java.lang.String, java.lang.String)
     */
    public Request<T> withParameter(String name, String value) {
        addParameter(name, value);
        return this;
    }

    /**
     * @see Request#getHttpMethod()
     */
    public HttpMethodName getHttpMethod() {
        return httpMethod;
    }

    /**
     * @see Request#setHttpMethod(HttpMethodName)
     */
    public void setHttpMethod(HttpMethodName httpMethod) {
        this.httpMethod = httpMethod;
    }

    /**
     * @see Request#getEndpoint()
     */
    public URI getEndpoint() {
        return endpoint;
    }

    /**
     * @see Request#setEndpoint(java.net.URI)
     */
    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @see Request#getServiceName()
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @see Request#getContent()
     */
    public InputStream getContent() {
        return content;
    }

    /**
     * @see Request#setContent(java.io.InputStream)
     */
    public void setContent(InputStream content) {
        this.content = content;
    }

    /**
     * @see Request#getTimeOffset
     */
    public int getTimeOffset() {
        return timeOffset;
    }

    /**
     * @see Request#setTimeOffset(int)
     */
    public void setTimeOffset(int timeOffset) {
        this.timeOffset = timeOffset;
    }

    /**
     * @see Request#setTimeOffset(int)
     */
    public Request<T> withTimeOffset(int timeOffset) {
        setTimeOffset(timeOffset);
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getHttpMethod()).append(" ");
        builder.append(getEndpoint()).append(" ");
        String resourcePath = getResourcePath();

        if (resourcePath == null) {
            builder.append("/");
        } else {
            if (!resourcePath.startsWith("/")) {
                builder.append("/");
            }
            builder.append(resourcePath);
        }
        builder.append(" ");
        if (!getParameters().isEmpty()) {
            builder.append("Parameters: (")
                   .append(Jackson.toJsonString(parameters));
        }

        if (!getHeaders().isEmpty()) {
            builder.append("Headers: (");
            for (String key : getHeaders().keySet()) {
                String value = getHeaders().get(key);
                builder.append(key).append(": ").append(value).append(", ");
            }
            builder.append(") ");
        }

        return builder.toString();
    }

    @Override
    public AwsRequestMetrics getAwsRequestMetrics() {
        return metrics;
    }

    @Override
    public void setAwsRequestMetrics(AwsRequestMetrics metrics) {
        if (this.metrics == null) {
            this.metrics = metrics;
        } else {
            throw new IllegalStateException("AWSRequestMetrics has already been set on this request");
        }
    }

    @SuppressWarnings("resource")
    @Override
    public InputStream getContentUnwrapped() {
        InputStream is = getContent();
        if (is == null) {
            return null;
        }
        // We want to disable the progress reporting when the stream is
        // consumed for signing purpose.
        while (is instanceof ProgressInputStream) {
            ProgressInputStream pris = (ProgressInputStream) is;
            is = pris.getWrappedInputStream();
        }
        return is;
    }

    @Override
    public ReadLimitInfo getReadLimitInfo() {
        return originalRequest;
    }

    @Override
    public Object getOriginalRequestObject() {
        return originalRequest;
    }
}
