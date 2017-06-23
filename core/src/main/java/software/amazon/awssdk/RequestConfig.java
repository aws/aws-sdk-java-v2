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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.metrics.RequestMetricCollector;

/**
 * Generic representation of request level configuration. The customer interface for specifying
 * request level configuration is a base request class with configuration methods.
 */
@SdkProtectedApi
public abstract class RequestConfig {

    /**
     * No op implementation to initalize request config in {@link DefaultRequest}.
     */
    public static final RequestConfig NO_OP = new AmazonWebServiceRequestAdapter(
            AmazonWebServiceRequest.NOOP);

    public abstract ProgressListener getProgressListener();

    public abstract RequestMetricCollector getRequestMetricsCollector();

    public abstract AwsCredentialsProvider getCredentialsProvider();

    /**
     * @return A non null map of custom headers to inject into the request.
     */
    public abstract Map<String, String> getCustomRequestHeaders();

    /**
     * @return A non null map of custom query parameters to inject into the request.
     */
    public abstract Map<String, List<String>> getCustomQueryParameters();

    public abstract Integer getClientExecutionTimeout();

    public abstract RequestClientOptions getRequestClientOptions();

    /**
     * @return String identifying the 'type' (i.e. operation) of the request. Used in metrics
     *     subsystem.
     */
    public abstract String getRequestType();

    /**
     * @return The original request object. May be delivered to various strategies or hooks for
     *     extra context. I.E. {@link RequestHandler} or {@link
     * RetryPolicy}.
     */
    public abstract Object getOriginalRequest();

}
