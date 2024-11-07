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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.internal.SystemSettingsCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.SystemSetting;
import software.amazon.awssdk.utils.ToString;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} that loads credentials from the
 * {@code AWS_ACCESS_KEY_ID}, {@code AWS_SECRET_ACCESS_KEY} and {@code AWS_SESSION_TOKEN} (optional)
 * <a href="https://en.wikipedia.org/wiki/Environment_variable">environment variables</a>.
 *
 * <p>
 * These environment variables may be populated automatically in some AWS service environments, like AWS Lambda:
 * <ul>
 *     <li>{@code AWS_ACCESS_KEY_ID} is the access key associated with your user or role.</li>
 *     <li>{@code AWS_SECRET_ACCESS_KEY} is the secret access key associated with your user or role.</li>
 *     <li>{@code AWS_SESSION_TOKEN} (optional) is the session token associated with your role.</li>
 * </ul>
 *
 * <p>
 * This credentials provider is included in the {@link DefaultCredentialsProvider}.
 *
 * <p>
 * This can be created using {@link EnvironmentVariableCredentialsProvider#create()}:
 * {@snippet :
 * EnvironmentVariableCredentialsProvider credentialsProvider =
 *    EnvironmentVariableCredentialsProvider.create(); // @link substring="create" target="#create()"
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class EnvironmentVariableCredentialsProvider extends SystemSettingsCredentialsProvider {
    private static final String PROVIDER_NAME = "EnvironmentVariableCredentialsProvider";

    private EnvironmentVariableCredentialsProvider() {
    }

    /**
     * Create a {@link EnvironmentVariableCredentialsProvider}.
     * <p>
     * {@snippet :
     * EnvironmentVariableCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();
     * }
     */
    public static EnvironmentVariableCredentialsProvider create() {
        return new EnvironmentVariableCredentialsProvider();
    }

    @Override
    protected Optional<String> loadSetting(SystemSetting setting) {
        // CHECKSTYLE:OFF - Customers should be able to specify a credentials provider that only looks at the environment
        // variables, but not the system properties. For that reason, we're only checking the environment variable here.
        return SystemSetting.getStringValueFromEnvironmentVariable(setting.environmentVariable());
        // CHECKSTYLE:ON
    }

    @Override
    protected String provider() {
        return PROVIDER_NAME;
    }

    @Override
    public String toString() {
        return ToString.create(PROVIDER_NAME);
    }
}
