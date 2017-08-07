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

package software.amazon.awssdk.http;

import java.net.URI;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;

public interface SdkHttpRequest extends SdkHttpHeaders {

    /**
     * Returns the path to the resource being requested.
     *
     * @return The path to the resource being requested.
     */
    String getResourcePath();

    /**
     * Returns a map of all parameters in this request.
     * <br/>
     * Should never be null, if there are no parameters an empty map is returned.
     *
     * @return A map of all parameters in this request.
     */
    @ReviewBeforeRelease("Should this be 'query parameters'? It took me a while to figure out what an HTTP parameter was.")
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
    SdkHttpMethod getHttpMethod();
}
