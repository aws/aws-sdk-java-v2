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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.internal.AmazonWebServiceRequestAdapter;

/**
 * Generic representation of request level configuration. The customer interface for specifying
 * request level configuration is a base request class with configuration methods.
 */
@SdkProtectedApi
@ReviewBeforeRelease("Clean up or delete - doesn't follow standards.")
public abstract class RequestConfig {

    public abstract AwsCredentialsProvider getCredentialsProvider();

    /**
     * @return A non null map of custom headers to inject into the request.
     */
    @ReviewBeforeRelease("Should be String, List<String> to match client config.")
    public abstract Map<String, String> getCustomRequestHeaders();

    /**
     * @return A non null map of custom query parameters to inject into the request.
     */
    public abstract Map<String, List<String>> getCustomQueryParameters();

    public abstract Integer getClientExecutionTimeout();

    public abstract RequestClientOptions getRequestClientOptions();

    /**
     * @return String identifying the 'type' (i.e. operation) of the request.
     */
    public abstract String getRequestType();

    /**
     * @return The original request object, before any modifications by {@link ExecutionInterceptor}s.
     */
    public abstract SdkRequest getOriginalRequest();

    /**
     *
     * @return Returns an empty, no-op implementation of request config.
     */
    public static RequestConfig empty() {
        return new AmazonWebServiceRequestAdapter(AmazonWebServiceRequest.NOOP);
    }

}
