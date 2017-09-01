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

package software.amazon.awssdk.core;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.http.HttpMethodName;

/**
 * Represents a request being sent to an Amazon Web Service, including the
 * parameters being sent as part of the request, the endpoint to which the
 * request should be sent, etc.
 * Members of this class should be considered read-only and not mutated
 *
 * @param <T> The type of original, user facing request represented by this
 *            request.
 */
@ReviewBeforeRelease("Shouldn't be needed after signer/base model refactor.")
public interface ImmutableRequest<T> {
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
}
