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

package software.amazon.awssdk.config;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Protocol;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link ClientConfiguration} that is guaranteed to be immutable and thread-safe.
 */
@SdkInternalApi
public abstract class ImmutableClientConfiguration implements ClientConfiguration {
    private final ClientOverrideConfiguration overrideConfiguration;
    private final AwsCredentialsProvider credentialsProvider;
    private final URI endpoint;
    private final LegacyClientConfiguration legacyConfiguration;

    /**
     * Copy the provided client configuration into an immutable version.
     */
    public ImmutableClientConfiguration(ClientConfiguration configuration) {
        this.overrideConfiguration = configuration.overrideConfiguration();
        this.credentialsProvider = configuration.credentialsProvider();
        this.endpoint = configuration.endpoint();

        validate();

        this.legacyConfiguration = initializeLegacyConfiguration();
    }

    /**
     * Validate that the provided optional is present, raising an exception if it is not.
     */
    protected final <T> T requireField(String field, T requiredConfiguration) {
        return Validate.notNull(requiredConfiguration, "The '%s' must be configured in the client builder.", field);
    }

    /**
     * Validate the contents of this configuration to ensure it includes all of the required fields.
     */
    private void validate() {
        // Ensure they have configured something that allows us to derive the endpoint
        Validate.validState(endpoint() != null, "The endpoint could not be determined.");

        requireField("overrideConfiguration.advancedOption[SIGNER_PROVIDER]",
                     overrideConfiguration().advancedOption(AdvancedClientOption.SIGNER_PROVIDER));
        requireField("overrideConfiguration.gzipEnabled", overrideConfiguration().gzipEnabled());
        requireField("overrideConfiguration.requestMetricCollector", overrideConfiguration().requestMetricCollector());
        requireField("overrideConfiguration.advancedOption[USER_AGENT_PREFIX]",
                     overrideConfiguration().advancedOption(AdvancedClientOption.USER_AGENT_PREFIX));
        requireField("overrideConfiguration.advancedOption[USER_AGENT_SUFFIX]",
                     overrideConfiguration().advancedOption(AdvancedClientOption.USER_AGENT_SUFFIX));
        requireField("overrideConfiguration.retryPolicy", overrideConfiguration().retryPolicy());
        requireField("credentialsProvider", credentialsProvider());
        requireField("endpoint", endpoint());
    }

    @Override
    public ClientOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
    }

    @Override
    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    @Override
    public URI endpoint() {
        return endpoint;
    }

    /**
     * Convert this client configuration into a legacy-style configuration object.
     */
    @Deprecated
    @ReviewBeforeRelease("This should be removed once we remove our reliance on the legacy client configuration object.")
    public LegacyClientConfiguration asLegacyConfiguration() {
        return this.legacyConfiguration;
    }

    /**
     * Convert this client configuration to a {@link LegacyClientConfiguration}.
     */
    private LegacyClientConfiguration initializeLegacyConfiguration() {
        LegacyClientConfiguration configuration = new LegacyClientConfiguration();

        copyOverrideConfiguration(configuration, overrideConfiguration());

        configuration.setProtocol(schemeToProtocol(endpoint().getScheme()).orElse(Protocol.HTTPS));

        return configuration;
    }

    private void copyOverrideConfiguration(LegacyClientConfiguration configuration,
                                          ClientOverrideConfiguration overrideConfiguration) {
        Optional.ofNullable(overrideConfiguration.totalExecutionTimeout())
                .ifPresent(d -> configuration.setClientExecutionTimeout(Math.toIntExact(d.toMillis())));

        Optional.ofNullable(overrideConfiguration.gzipEnabled())
                .ifPresent(configuration::setUseGzip);

        overrideConfiguration.additionalHttpHeaders().forEach((header, values) -> {
            if (values.size() > 1) {
                throw new IllegalArgumentException("Multiple values under the same header are not supported at this time.");
            }
            values.forEach(value -> configuration.addHeader(header, value));
        });

        Optional.ofNullable(overrideConfiguration.advancedOption(AdvancedClientOption.USER_AGENT_PREFIX))
                .ifPresent(configuration::setUserAgentPrefix);

        Optional.ofNullable(overrideConfiguration.advancedOption(AdvancedClientOption.USER_AGENT_SUFFIX))
                .ifPresent(configuration::setUserAgentSuffix);

        Optional.ofNullable(overrideConfiguration.retryPolicy())
                .ifPresent(configuration::setRetryPolicy);
    }

    private Optional<Protocol> schemeToProtocol(String scheme) {
        return Arrays.stream(Protocol.values()).filter(p -> scheme.equals(p.toString())).findFirst();
    }
}
