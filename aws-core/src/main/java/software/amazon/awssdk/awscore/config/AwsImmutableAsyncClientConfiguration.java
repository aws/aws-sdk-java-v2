/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.config;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.config.SdkImmutableAsyncClientConfiguration;

/**
 * An implementation of {@link AwsAsyncClientConfiguration} that is guaranteed to be immutable and thread-safe.
 */
@SdkInternalApi
public final class AwsImmutableAsyncClientConfiguration extends SdkImmutableAsyncClientConfiguration implements
                                                                                                     AwsAsyncClientConfiguration {

    private final AwsCredentialsProvider credentialsProvider;

    public AwsImmutableAsyncClientConfiguration(AwsAsyncClientConfiguration configuration) {
        super(configuration);
        this.credentialsProvider = configuration.credentialsProvider();
        validate();
    }

    @Override
    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    private void validate() {
        requireField("credentialsProvider", credentialsProvider());
    }
}
