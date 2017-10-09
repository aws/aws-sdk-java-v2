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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.event.ProgressListener;

/**
 * Adapts the configuration present in {@link AmazonWebServiceRequest} to {@link RequestConfig}.
 */
@SdkInternalApi
public final class AmazonWebServiceRequestAdapter extends RequestConfig {

    /**
     * {@link Class#getSimpleName()} is a little expensive. Cache the result for request objects we come across.
     */
    private static final Map<Class<?>, String> SIMPLE_NAME_CACHE = new ConcurrentHashMap<>();

    private final SdkRequest request;
    private final String simpleName;

    public AmazonWebServiceRequestAdapter(SdkRequest request) {
        this.request = request;
        this.simpleName = SIMPLE_NAME_CACHE.computeIfAbsent(request.getClass(), Class::getSimpleName);
    }

    @Override
    public ProgressListener getProgressListener() {
        // FIXME(dongie)
        return null;
    }

    @Override
    public AwsCredentialsProvider getCredentialsProvider() {
        // FIXME(dongie)
        return null;
    }

    @Override
    public Map<String, String> getCustomRequestHeaders() {
        // FIXME(dongie)
        return null;
    }

    @Override
    public Map<String, List<String>> getCustomQueryParameters() {
        // FIXME(dongie)
        return null;
    }

    @Override
    public Integer getClientExecutionTimeout() {
        // FIXME(dongie)
        return null;
    }

    @Override
    public RequestClientOptions getRequestClientOptions() {
        // FIXME(dongie)
        return null;
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
