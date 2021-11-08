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
import software.amazon.awssdk.regions.Region;
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
         * Configure the region for which the requests should be signed.
         *
         * <p>If this is not specified, the SDK will attempt to identify the endpoint automatically using the following logic:
         * <ol>
         *     <li>Check the 'aws.region' system property for the region.</li>
         *     <li>Check the 'AWS_REGION' environment variable for the region.</li>
         *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the region.</li>
         *     <li>If running in EC2, check the EC2 metadata service for the region.</li>
         * </ol>
         *
         * <p>If the region is not found in any of the locations above, an exception will be thrown at {@link #build()}
         * time.
         */
        Builder region(Region region);

        /**
         * Configure the credentials that should be used for request signing.
         *
         * <p>The default provider will attempt to identify the credentials automatically using the following checks:
         * <ol>
         *   <li>Java System Properties - <code>aws.accessKeyId</code> and <code>aws.secretKey</code></li>
         *   <li>Environment Variables - <code>AWS_ACCESS_KEY_ID</code> and <code>AWS_SECRET_ACCESS_KEY</code></li>
         *   <li>Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI</li>
         *   <li>Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI
         *   environment variable is set and security manager has permission to access the variable.</li>
         *   <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
         * </ol>
         *
         * <p>If the credentials are not found in any of the locations above, an exception will be thrown at {@link #build()}
         * time.
         * </p>
         */
        Builder credentialsProvider(AwsCredentialsProvider credentialsProvider);

        /**
         * Configure whether the SDK should use the AWS dualstack endpoint.
         *
         * <p>If this is not specified, the SDK will attempt to determine whether the dualstack endpoint should be used
         * automatically using the following logic:
         * <ol>
         *     <li>Check the 'aws.useDualstackEndpoint' system property for 'true' or 'false'.</li>
         *     <li>Check the 'AWS_USE_DUALSTACK_ENDPOINT' environment variable for 'true' or 'false'.</li>
         *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the 'use_dualstack_endpoint'
         *     property set to 'true' or 'false'.</li>
         * </ol>
         *
         * <p>If the setting is not found in any of the locations above, 'false' will be used.
         */
        Builder dualstackEnabled(Boolean dualstackEnabled);

        /**
         * Configure whether the SDK should use the AWS fips endpoint.
         *
         * <p>If this is not specified, the SDK will attempt to determine whether the fips endpoint should be used
         * automatically using the following logic:
         * <ol>
         *     <li>Check the 'aws.useFipsEndpoint' system property for 'true' or 'false'.</li>
         *     <li>Check the 'AWS_USE_FIPS_ENDPOINT' environment variable for 'true' or 'false'.</li>
         *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the 'use_fips_endpoint'
         *     property set to 'true' or 'false'.</li>
         * </ol>
         *
         * <p>If the setting is not found in any of the locations above, 'false' will be used.
         */
        Builder fipsEnabled(Boolean fipsEnabled);

        /**
         * Configure an endpoint that should be used in the pre-signed requests. This will override the endpoint that is usually
         * determined by the {@link #region(Region)} and {@link #dualstackEnabled(Boolean)} settings.
         */
        Builder endpointOverride(URI endpointOverride);

        /**
         * Build the presigner using the configuration on this builder.
         */
        SdkPresigner build();
    }
}
