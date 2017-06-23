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

package software.amazon.awssdk.client;

import java.net.URI;
import java.util.List;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.runtime.auth.SignerProvider;

/**
 * Provides access to all params needed in a synchronous AWS service client constructor. Abstract
 * to allow additions to the params while maintaining backwards compatibility.
 */
@SdkProtectedApi
public abstract class AwsSyncClientParams {

    public abstract AwsCredentialsProvider getCredentialsProvider();

    public abstract LegacyClientConfiguration getClientConfiguration();

    public abstract RequestMetricCollector getRequestMetricCollector();

    public abstract List<RequestHandler> getRequestHandlers();

    public abstract SdkHttpClient sdkHttpClient();

    public SignerProvider getSignerProvider() {
        // Not currently used by AWS clients. The builder uses setRegion to configure endpoint
        // and signer and does not support custom endpoints or signers.
        return null;
    }

    public URI getEndpoint() {
        // Not currently used by AWS clients. The builder uses setRegion to configure endpoint
        // and signer and does not support custom endpoints or signers.
        return null;
    }

    public RetryPolicy getRetryPolicy() {
        final LegacyClientConfiguration config = getClientConfiguration();
        return new RetryPolicyAdapter(config.getRetryPolicy(), config);
    }
}
