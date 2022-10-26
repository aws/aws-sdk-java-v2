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

package software.amazon.awssdk.awscore.client.config;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.regions.Region;

@SdkProtectedApi
public final class AwsClientOption<T> extends ClientOption<T> {
    /**
     * @see AwsClientBuilder#credentialsProvider(AwsCredentialsProvider)
     */
    public static final AwsClientOption<AwsCredentialsProvider> CREDENTIALS_PROVIDER =
            new AwsClientOption<>(AwsCredentialsProvider.class);

    /**
     * AWS Region the client was configured with. Note that this is not always the signing region in the case of global
     * services like IAM.
     */
    public static final AwsClientOption<Region> AWS_REGION = new AwsClientOption<>(Region.class);

    /**
     * AWS Region to be used for signing the request. This is not always same as {@link #AWS_REGION} in case of global services.
     */
    public static final AwsClientOption<Region> SIGNING_REGION = new AwsClientOption<>(Region.class);

    /**
     * Whether the SDK should resolve dualstack endpoints instead of default endpoints. See
     * {@link AwsClientBuilder#dualstackEnabled(Boolean)}.
     */
    public static final AwsClientOption<Boolean> DUALSTACK_ENDPOINT_ENABLED = new AwsClientOption<>(Boolean.class);

    /**
     * Whether the SDK should resolve fips endpoints instead of default endpoints. See
     * {@link AwsClientBuilder#fipsEnabled(Boolean)}.
     */
    public static final ClientOption<Boolean> FIPS_ENDPOINT_ENABLED = new AwsClientOption<>(Boolean.class);

    /**
     * Scope name to use during signing of a request.
     */
    public static final AwsClientOption<String> SERVICE_SIGNING_NAME = new AwsClientOption<>(String.class);

    /**
     * The first part of the URL in the DNS name for the service. Eg. in the endpoint "dynamodb.amazonaws.com", this is the
     * "dynamodb".
     *
     * For standard services, this should match the "endpointPrefix" field in the AWS model.
     */
    public static final AwsClientOption<String> ENDPOINT_PREFIX = new AwsClientOption<>(String.class);

    /**
     * Option to specify the {@link DefaultsMode}
     */
    public static final AwsClientOption<DefaultsMode> DEFAULTS_MODE = new AwsClientOption<>(DefaultsMode.class);

    /**
     * Option to specify whether global endpoint should be used.
     */
    public static final AwsClientOption<Boolean> USE_GLOBAL_ENDPOINT = new AwsClientOption<>(Boolean.class);

    /**
     * Option to specific the {@link SdkTokenProvider} to use for bearer token authorization.
     */
    public static final AwsClientOption<SdkTokenProvider> TOKEN_PROVIDER = new AwsClientOption<>(SdkTokenProvider.class);

    private AwsClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
