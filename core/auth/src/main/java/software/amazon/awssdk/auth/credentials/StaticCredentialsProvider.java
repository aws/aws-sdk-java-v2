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
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link AwsCredentialsProvider} that returns a set implementation of {@link AwsCredentials}.
 */
@SdkPublicApi
public final class StaticCredentialsProvider implements AwsCredentialsProvider {
    private static final String PROVIDER_NAME = BusinessMetricFeatureId.CREDENTIALS_CODE.value();
    private final AwsCredentials credentials;

    private StaticCredentialsProvider(AwsCredentials credentials) {
        Validate.notNull(credentials, "Credentials must not be null.");
        this.credentials = withProviderName(credentials);
    }

    private AwsCredentials withProviderName(AwsCredentials credentials) {
        if (credentials instanceof AwsBasicCredentials) {
            AwsBasicCredentials basicCreds = (AwsBasicCredentials) credentials;
            if (basicCreds.providerName().isPresent() && 
                BusinessMetricFeatureId.CREDENTIALS_PROFILE.value().equals(basicCreds.providerName().get())) {
                return basicCreds;
            }
            return basicCreds.copy(c -> c.providerName(PROVIDER_NAME));
        }
        if (credentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCreds = (AwsSessionCredentials) credentials;
            if (sessionCreds.providerName().isPresent() && 
                BusinessMetricFeatureId.CREDENTIALS_PROFILE.value().equals(sessionCreds.providerName().get())) {
                return sessionCreds;
            }
            return sessionCreds.copy(c -> c.providerName(PROVIDER_NAME));
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
