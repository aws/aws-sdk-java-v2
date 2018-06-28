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

package software.amazon.awssdk.core;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpMethodName;

/**
 * Represents a request being sent to an Amazon Web Service, including the
 * parameters being sent as part of the request, the endpoint to which the
 * request should be sent, etc.
 * <p>
 * This class is only intended for use inside the AWS client libraries and
 * request handlers. Users of the AWS SDK for Java should not implement this
 * interface.
 *
 * @param <T>
 *            The type of original, user facing request represented by this
 *            request.
 */
@SdkProtectedApi
public interface Request<T> {
    /**
     * Returns a map of all the headers included in this request.
     *
     * @return A map of all the headers included in this request.
     */
    Map<String, String> getHeaders();

    /**
     * Returns the path to the resource being requested.
     *
     * @return The path to the resource being requested.
     */
    String getResourcePath();

    /**
     * Returns a map of all parameters in this request.
     *
     * @return A map of all parameters in this request.
     */
    Map<String, List<String>> getParameters();

    /**
     * Returns the service endpoint (ex: "https://ec2.amazonaws.com") to which
     * this request should be sent.
     *
     * @return The service endpoint to which this request should be sent.
     */
    URI getEndpoint();

    /**
     * Returns the HTTP method (GET, POST, etc) to use when sending this
     * request.
     *
     * @return The HTTP method to use when sending this request.
     */
    HttpMethodName getHttpMethod();

    /**
     * Returns the optional value for time offset for this request.  This
     * will be used by the signer to adjust for potential clock skew.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @return The optional value for time offset (in seconds) for this request.
     */
    int getTimeOffset();

    /**
     * Returns the optional stream containing the payload data to include for
     * this request. Not all requests will contain payload data.
     *
     * @return The optional stream containing the payload data to include for
     *         this request.
     */
    InputStream getContent();

    /**
     * request object is representing.
     */
    T getOriginalRequest();

    /**
     * Sets the specified header for this request.
     *
     * @param name
     *            The name of the header to set.
     * @param value
     *            The header's value.
     */
    void addHeader(String name, String value);

    /**
     * Adds the specified request parameter to this request.
     *
     * @param name
     *            The name of the request parameter.
     * @param value
     *            The value of the request parameter.
     */
    void addParameter(String name, String value);

    /**
     * Sets the optional stream containing the payload data to include for this
     * request. This is used, for example, for S3 chunk encoding.
     *
     * @param content
     *            The optional stream containing the payload data to include for
     *            this request.
     */
    void setContent(InputStream content);

    /**
     * Sets all headers, clearing any existing ones.
     */
    void setHeaders(Map<String, String> headers);

    /**
     * Sets the path to the resource being requested.
     *
     * @param path
     *            The path to the resource being requested.
     */
    void setResourcePath(String path);

    /**
     * Adds the specified request parameter to this request, and returns the
     * updated request object.
     *
     * @param name
     *            The name of the request parameter.
     * @param value
     *            The value of the request parameter.
     *
     * @return The updated request object.
     */
    Request<T> withParameter(String name, String value);

    /**
     * Sets all parameters, clearing any existing values.
     *
     * Note that List values within the parameters Map must use an implementation that supports null
     * values.
     */
    void setParameters(Map<String, List<String>> parameters);

    /**
     * Adds the specified request parameter and list of values to this request.
     *
     * @param name
     *            The name of the request parameter.
     * @param values
     *            The value of the request parameter.
     */
    void addParameters(String name, List<String> values);

    /**
     * Sets the service endpoint (ex: "https://ec2.amazonaws.com") to which this
     * request should be sent.
     *
     * @param endpoint
     *            The service endpoint to which this request should be sent.
     */
    void setEndpoint(URI endpoint);

    /**
     * Sets the HTTP method (GET, POST, etc) to use when sending this request.
     *
     * @param httpMethod
     *            The HTTP method to use when sending this request.
     */
    void setHttpMethod(HttpMethodName httpMethod);

    /**
     * @return The name of the Amazon service this request is for. This is used
     *         as the service name set in request metrics and service
     *         exceptions.
     *
     * @see SdkServiceException#serviceName()
     */
    String getServiceName();

    /**
     * Sets the optional value for time offset for this request.  This
     * will be used by the signer to adjust for potential clock skew.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @param timeOffset
     *            The optional value for time offset (in seconds) for this request.
     */
    void setTimeOffset(int timeOffset);

    /**
     * Sets the optional value for time offset for this request.  This
     * will be used by the signer to adjust for potential clock skew.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @return The updated request object.
     */
    Request<T> withTimeOffset(int timeOffset);
}
