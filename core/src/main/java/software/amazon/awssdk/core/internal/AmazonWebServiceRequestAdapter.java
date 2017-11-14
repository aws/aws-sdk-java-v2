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

package software.amazon.awssdk.core.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.AmazonWebServiceRequest;
import software.amazon.awssdk.core.RequestClientOptions;
import software.amazon.awssdk.core.RequestConfig;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;

/**
 * Adapts the configuration present in {@link AmazonWebServiceRequest} to {@link RequestConfig}.
 */
@SdkInternalApi
public final class AmazonWebServiceRequestAdapter extends RequestConfig {

    /**
     * {@link Class#getSimpleName()} is a little expensive. Cache the result for request objects we come across.
     */
    private static final Map<Class<?>, String> SIMPLE_NAME_CACHE = new ConcurrentHashMap<>();

    private final AmazonWebServiceRequest request;
    private final String simpleName;

    public AmazonWebServiceRequestAdapter(AmazonWebServiceRequest request) {
        this.request = request;
        this.simpleName = SIMPLE_NAME_CACHE.computeIfAbsent(request.getClass(), Class::getSimpleName);
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
        return simpleName;
    }

    @Override
    public SdkRequest getOriginalRequest() {
        return request;
    }
}
