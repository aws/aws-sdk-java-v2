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

package software.amazon.awssdk.opensdk.protect.client;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.opensdk.BaseRequest;
import software.amazon.awssdk.opensdk.SdkRequestConfig;

/**
 * Adapts {@link SdkRequestConfig} to {@link RequestConfig}.
 */
@SdkProtectedApi
class RequestConfigAdapter extends RequestConfig {

    private final BaseRequest request;
    private final SdkRequestConfig sdkRequestConfig;
    private final RequestClientOptions clientOptions = new RequestClientOptions();

    public RequestConfigAdapter(BaseRequest request) {
        this.request = request;
        this.sdkRequestConfig = request.sdkRequestConfig();
    }

    /**
     * Progress listeners are not yet supported for API Gateway clients.
     */
    @Override
    public ProgressListener getProgressListener() {
        return ProgressListener.NOOP;
    }

    /**
     * Request level metrics collector is not yet supported for API Gateway clients.
     */
    @Override
    public RequestMetricCollector getRequestMetricsCollector() {
        return RequestMetricCollector.NONE;
    }

    /**
     * Not applicable for API Gateway.
     */
    @Override
    public AwsCredentialsProvider getCredentialsProvider() {
        return null;
    }

    @Override
    public Map<String, String> getCustomRequestHeaders() {
        return sdkRequestConfig.getCustomHeaders();
    }

    @Override
    public Map<String, List<String>> getCustomQueryParameters() {
        return sdkRequestConfig.getCustomQueryParams();
    }

    @Override
    public Integer getClientExecutionTimeout() {
        return sdkRequestConfig.getTotalExecutionTimeout();
    }

    @Override
    public RequestClientOptions getRequestClientOptions() {
        return clientOptions;
    }

    @Override
    public String getRequestType() {
        return request.getClass().getSimpleName();
    }

    @Override
    public Object getOriginalRequest() {
        return request;
    }
}
