/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.config.defaults;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.config.AdvancedClientOption.SIGNER_PROVIDER;

import java.net.URI;
import org.junit.Test;
import software.amazon.awssdk.auth.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.config.ImmutableAsyncClientConfiguration;
import software.amazon.awssdk.config.ImmutableSyncClientConfiguration;
import software.amazon.awssdk.config.MutableClientConfiguration;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;

/**
 * Validate functionality of {@link GlobalClientConfigurationDefaults}.
 */
public class GlobalClientConfigurationDefaultsTest {
    @Test
    public void globalDefaultsIncludeExpectedValues() {
        // The global defaults should include every field except for those defined by the builder or the service. Specifically,
        // all required Client*Configuration values should be set, but not endpoints, credential providers, etc.

        GlobalClientConfigurationDefaults globalDefaults = new GlobalClientConfigurationDefaults();

        // Add the required values not expected to be included in the global configuration.
        ClientConfigurationDefaults configCompleter = new ClientConfigurationDefaults() {
            @Override
            protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                assertThat(builder.build().advancedOption(SIGNER_PROVIDER)).isNull();
                builder.advancedOption(SIGNER_PROVIDER, new NoOpSignerProvider());
            }

            @Override
            protected AwsCredentialsProvider getCredentialsDefault() {
                return new AnonymousCredentialsProvider();
            }

            @Override
            protected URI getEndpointDefault() {
                return URI.create("http://example.com");
            }
        };

        MutableClientConfiguration mockAsyncCustomerConfig = new MutableClientConfiguration();
        MutableClientConfiguration mockSyncCustomerConfig = new MutableClientConfiguration();

        globalDefaults.applyAsyncDefaults(mockAsyncCustomerConfig);
        configCompleter.applyAsyncDefaults(mockAsyncCustomerConfig);

        globalDefaults.applySyncDefaults(mockSyncCustomerConfig);
        configCompleter.applySyncDefaults(mockSyncCustomerConfig);

        // Make sure we can create an Immutable*ClientConfiguration with the result. Otherwise, it will throw an exception that a
        // required field is missing.
        new ImmutableAsyncClientConfiguration(mockAsyncCustomerConfig);
        new ImmutableSyncClientConfiguration(mockSyncCustomerConfig);
    }
}
