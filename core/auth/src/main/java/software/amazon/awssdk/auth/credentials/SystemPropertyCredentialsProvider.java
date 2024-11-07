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
 * {@code aws.accessKeyId}, {@code aws.secretAccessKey} and {@code aws.sessionToken} (optional) system properties.
 *
 * <ul>
 *     <li>{@code aws.accessKeyId} is the access key associated with your user or role.</li>
 *     <li>{@code aws.secretAccessKey} is the secret access key associated with your user or role.</li>
 *     <li>{@code aws.sessionToken} (optional) is the session token associated with your role.</li>
 * </ul>
 *
 * <p>
 * This credentials provider is included in the {@link DefaultCredentialsProvider}.
 *
 * <p>
 * This can be created using {@link #create()}:
 * {@snippet :
 * SystemPropertyCredentialsProvider credentialsProvider =
 *    SystemPropertyCredentialsProvider.create(); // @link substring="create" target="#create()"
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
 */
@SdkPublicApi
public final class SystemPropertyCredentialsProvider extends SystemSettingsCredentialsProvider {
    private static final String PROVIDER_NAME = "SystemPropertyCredentialsProvider";

    private SystemPropertyCredentialsProvider() {
    }

    /**
     * Create a {@link SystemPropertyCredentialsProvider}.
     * <p>
     * {@snippet :
     * SystemPropertyCredentialsProvider credentialsProvider = SystemPropertyCredentialsProvider.create();
     * }
     */
    public static SystemPropertyCredentialsProvider create() {
        return new SystemPropertyCredentialsProvider();
    }

    @Override
    protected Optional<String> loadSetting(SystemSetting setting) {
        // CHECKSTYLE:OFF - Customers should be able to specify a credentials provider that only looks at the system properties,
        // but not the environment variables. For that reason, we're only checking the system properties here.
        return Optional.ofNullable(System.getProperty(setting.property()));
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
