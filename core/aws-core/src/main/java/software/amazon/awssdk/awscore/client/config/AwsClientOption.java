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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.regions.Region;

@SdkProtectedApi
public final class AwsClientOption<T> extends ClientOption<T> {
    /**
     * This option is deprecated in favor of {@link #CREDENTIALS_IDENTITY_PROVIDER}.
     * @see AwsClientBuilder#credentialsProvider(AwsCredentialsProvider)
     */
    @Deprecated
    // smithy codegen TODO: This could be removed when doing a minor version bump where we told customers we'll be breaking
    //  protected APIs. Postpone this to when we do Smithy code generator migration, where we'll likely have to start
    //  breaking a lot of protected things.
    public static final AwsClientOption<AwsCredentialsProvider> CREDENTIALS_PROVIDER =
            new AwsClientOption<>(AwsCredentialsProvider.class);

    /**
     * @see AwsClientBuilder#credentialsProvider(IdentityProvider)
     */
    public static final AwsClientOption<IdentityProvider<? extends AwsCredentialsIdentity>> CREDENTIALS_IDENTITY_PROVIDER =
        new AwsClientOption<>(new UnsafeValueType(IdentityProvider.class));

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
     * AWS Sigv4a  signing region set. This is used to compute the signing region for Sigv4a requests.
     */
    public static final AwsClientOption<Set<String>> AWS_SIGV4A_SIGNING_REGION_SET =
        new AwsClientOption<>(new UnsafeValueType(Set.class));

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
     * Configuration of the DEFAULTS_MODE. Unlike {@link #DEFAULTS_MODE}, this may be {@link DefaultsMode#AUTO}.
     */
    public static final AwsClientOption<DefaultsMode> CONFIGURED_DEFAULTS_MODE = new AwsClientOption<>(DefaultsMode.class);

    /**
     * Option used by the rest of the SDK to read the {@link DefaultsMode}. This will never be {@link DefaultsMode#AUTO}.
     */
    public static final AwsClientOption<DefaultsMode> DEFAULTS_MODE = new AwsClientOption<>(DefaultsMode.class);

    /**
     * Option used by the rest of the SDK to read the {@link DefaultsMode}. This will never be {@link DefaultsMode#AUTO}.
     */
    public static final AwsClientOption<AccountIdEndpointMode> ACCOUNT_ID_ENDPOINT_MODE =
        new AwsClientOption<>(AccountIdEndpointMode.class);

    /**
     * Option to specify whether global endpoint should be used.
     */
    public static final AwsClientOption<Boolean> USE_GLOBAL_ENDPOINT = new AwsClientOption<>(Boolean.class);

    /**
     * Option to specific the {@link SdkTokenProvider} to use for bearer token authorization.
     * This option is deprecated in favor or {@link #TOKEN_IDENTITY_PROVIDER}
     */
    @Deprecated
    public static final AwsClientOption<SdkTokenProvider> TOKEN_PROVIDER = new AwsClientOption<>(SdkTokenProvider.class);

    /**
     * Option to specific the {@link SdkTokenProvider} to use for bearer token authorization.
     */
    public static final AwsClientOption<IdentityProvider<? extends TokenIdentity>> TOKEN_IDENTITY_PROVIDER =
        new AwsClientOption<>(new UnsafeValueType(IdentityProvider.class));

    private AwsClientOption(Class<T> valueClass) {
        super(valueClass);
    }

    private AwsClientOption(UnsafeValueType valueType) {
        super(valueType);
    }
}
