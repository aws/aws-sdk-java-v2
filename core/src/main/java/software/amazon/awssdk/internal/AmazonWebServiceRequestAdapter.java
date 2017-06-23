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

package software.amazon.awssdk.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;

/**
 * Adapts the configuration present in {@link AmazonWebServiceRequest} to {@link RequestConfig}.
 */
@SdkInternalApi
public final class AmazonWebServiceRequestAdapter extends RequestConfig {

    private final AmazonWebServiceRequest request;

    public AmazonWebServiceRequestAdapter(AmazonWebServiceRequest request) {
        this.request = request;
    }

    @Override
    public ProgressListener getProgressListener() {
        return request.getGeneralProgressListener();
    }

    @Override
    public RequestMetricCollector getRequestMetricsCollector() {
        return request.getRequestMetricCollector();
    }

    @Override
    public AwsCredentialsProvider getCredentialsProvider() {
        return request.getRequestCredentialsProvider();
    }

    @Override
    public Map<String, String> getCustomRequestHeaders() {
        return (request.getCustomRequestHeaders() == null) ? Collections.<String, String>emptyMap() :
               request.getCustomRequestHeaders();
    }

    @Override
    public Map<String, List<String>> getCustomQueryParameters() {
        return (request.getCustomQueryParameters() == null) ? Collections.<String, List<String>>emptyMap() :
               request.getCustomQueryParameters();
    }

    @Override
    public Integer getClientExecutionTimeout() {
        return request.getSdkClientExecutionTimeout();
    }

    @Override
    public RequestClientOptions getRequestClientOptions() {
        return request.getRequestClientOptions();
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
