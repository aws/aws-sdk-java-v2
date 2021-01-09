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

package software.amazon.awssdk.testutils.service.http;

import java.util.List;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;

public interface MockHttpClient {
    /**
     * Resets this mock by clearing any captured requests and wiping any stubbed responses.
     */
    void reset();

    /**
     * Sets up the next HTTP response that will be returned by the mock. Removes responses previously added to the mock.
     */
    void stubNextResponse(HttpExecuteResponse nextResponse);

    /**
     * Sets the next set of HTTP responses that will be returned by the mock. Removes responses previously added to the mock.
     */
    void stubResponses(HttpExecuteResponse... responses);

    /**
     * Get the last request called on the mock.
     */
    SdkHttpRequest getLastRequest();

    /**
     * Get all requests called on the mock.
     */
    List<SdkHttpRequest> getRequests();

}
