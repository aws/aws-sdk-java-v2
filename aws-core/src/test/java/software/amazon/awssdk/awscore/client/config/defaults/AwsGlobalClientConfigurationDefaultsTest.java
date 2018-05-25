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

package software.amazon.awssdk.awscore.client.config.defaults;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.config.SdkAdvancedClientOption.SIGNER_PROVIDER;

import java.net.URI;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.config.AwsImmutableAsyncClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsImmutableSyncClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsMutableClientConfiguration;
import software.amazon.awssdk.awscore.config.defaults.AwsClientConfigurationDefaults;
import software.amazon.awssdk.awscore.config.defaults.AwsGlobalClientConfigurationDefaults;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.auth.NoOpSignerProvider;

/**
 * Validate functionality of {@link AwsGlobalClientConfigurationDefaults}.
 */
public class AwsGlobalClientConfigurationDefaultsTest {
    @Test
    public void globalDefaultsIncludeExpectedValues() {
        // The global defaults should include every field except for those defined by the builder or the service. Specifically,
        // all required Client*Configuration values should be set, but not endpoints, credential providers, etc.

        AwsGlobalClientConfigurationDefaults globalDefaults = new AwsGlobalClientConfigurationDefaults();

        // Add the required values not expected to be included in the global configuration.
        AwsClientConfigurationDefaults configCompleter = new AwsClientConfigurationDefaults() {
            @Override
            protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                assertThat(builder.build().advancedOption(SIGNER_PROVIDER)).isNull();
                builder.advancedOption(SIGNER_PROVIDER, new NoOpSignerProvider());
            }

            @Override
            protected AwsCredentialsProvider getCredentialsDefault() {
                return AnonymousCredentialsProvider.create();
            }

            @Override
            protected URI getEndpointDefault() {
                return URI.create("http://example.com");
            }

        };

        AwsMutableClientConfiguration mockAsyncCustomerConfig = new AwsMutableClientConfiguration();
        AwsMutableClientConfiguration mockSyncCustomerConfig = new AwsMutableClientConfiguration();

        globalDefaults.applyAsyncDefaults(mockAsyncCustomerConfig);
        configCompleter.applyAsyncDefaults(mockAsyncCustomerConfig);

        globalDefaults.applySyncDefaults(mockSyncCustomerConfig);
        configCompleter.applySyncDefaults(mockSyncCustomerConfig);

        // Make sure we can create an Immutable*ClientConfiguration with the result. Otherwise, it will throw an exception that a
        // required field is missing.
        new AwsImmutableAsyncClientConfiguration(mockAsyncCustomerConfig);
        new AwsImmutableSyncClientConfiguration(mockSyncCustomerConfig);
    }
}
