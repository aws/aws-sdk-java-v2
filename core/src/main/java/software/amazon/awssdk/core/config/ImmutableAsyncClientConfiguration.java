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

package software.amazon.awssdk.config;

import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * An implementation of {@link AsyncClientConfiguration} that is guaranteed to be immutable and thread-safe.
 */
@SdkInternalApi
public final class ImmutableAsyncClientConfiguration extends ImmutableClientConfiguration implements AsyncClientConfiguration {

    private final ScheduledExecutorService asyncExecutorService;
    private final SdkAsyncHttpClient asyncHttpClient;

    public ImmutableAsyncClientConfiguration(AsyncClientConfiguration configuration) {
        super(configuration);
        this.asyncExecutorService = configuration.asyncExecutorService();
        this.asyncHttpClient = configuration.asyncHttpClient();

        validate();
    }

    private void validate() {
        requireField("asyncExecutorService", asyncExecutorService());
    }

    @Override
    public ScheduledExecutorService asyncExecutorService() {
        return asyncExecutorService;
    }

    @Override
    public SdkAsyncHttpClient asyncHttpClient() {
        return asyncHttpClient;
    }
}
