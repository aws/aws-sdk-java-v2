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

package software.amazon.awssdk.awscore.presigner;

import java.net.URI;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * The base interface for all SDK presigners.
 */
@SdkPublicApi
public interface SdkPresigner extends SdkAutoCloseable {
    /**
     * Close this presigner, releasing any resources it might have acquired. It is recommended to invoke this method whenever
     * the presigner is done being used, to prevent resource leaks.
     * <p/>
     * For example, some {@link AwsCredentialsProvider} implementations hold resources that could be released by this method.
     */
    @Override
    void close();

    /**
     * The base interface for all SDK presigner builders.
     */
    @SdkPublicApi
    interface Builder {
        /**
         * Configure the region that should be used for request signing.
         * <p/>
         * If this is not set, the {@link DefaultAwsRegionProviderChain} will be consulted to determine the region.
         */
        Builder region(Region region);

        /**
         * Configure the credentials that should be used for request signing.
         * <p/>
         * If this is not set, the {@link DefaultCredentialsProvider} will be used.
         */
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        /**
         * Configure an endpoint that should be used in the pre-signed requests. This will override the endpoint that is usually
         * determined by the {@link #region(Region)}.
         */
        Builder endpointOverride(URI endpointOverride);

        /**
         * Build the presigner using the configuration on this builder.
         */
        SdkPresigner build();
    }
}
