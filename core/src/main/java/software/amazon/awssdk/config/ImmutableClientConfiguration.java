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

    /**
     * Copy the provided client configuration into an immutable version.
     */
    public ImmutableClientConfiguration(ClientConfiguration configuration) {
        this.overrideConfiguration = configuration.overrideConfiguration();
        this.credentialsProvider = configuration.credentialsProvider();
        this.endpoint = configuration.endpoint();

        validate();
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
}
