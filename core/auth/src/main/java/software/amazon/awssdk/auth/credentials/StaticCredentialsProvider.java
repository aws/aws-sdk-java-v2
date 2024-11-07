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

package software.amazon.awssdk.auth.credentials;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} implementation that returns a static set of
 * credentials. This has been superseded by {@link IdentityProvider#staticAwsCredentials(AwsCredentialsIdentity)}.
 *
 * <p>
 * To avoid unnecessary churn this class has not been marked as deprecated, but it's recommended to use
 * {@link IdentityProvider#staticAwsCredentials} when defining generic credential providers because it provides the same
 * functionality with considerably fewer dependencies.
 */
@SdkPublicApi
public final class StaticCredentialsProvider implements AwsCredentialsProvider {
    private static final String PROVIDER_NAME = "StaticCredentialsProvider";
    private final AwsCredentials credentials;

    private StaticCredentialsProvider(AwsCredentials credentials) {
        Validate.notNull(credentials, "Credentials must not be null.");
        this.credentials = withProviderName(credentials);
    }

    private AwsCredentials withProviderName(AwsCredentials credentials) {
        if (credentials instanceof AwsBasicCredentials) {
            return ((AwsBasicCredentials) credentials).copy(c -> c.providerName(PROVIDER_NAME));
        }
        if (credentials instanceof AwsSessionCredentials) {
            return ((AwsSessionCredentials) credentials).copy(c -> c.providerName(PROVIDER_NAME));
        }
        return credentials;
    }

    /**
     * Create a credentials provider that always returns the provided set of credentials.
     */
    public static StaticCredentialsProvider create(AwsCredentials credentials) {
        return new StaticCredentialsProvider(credentials);
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentials;
    }

    @Override
    public String toString() {
        return ToString.builder(PROVIDER_NAME)
                       .add("credentials", credentials)
                       .build();
    }
}
