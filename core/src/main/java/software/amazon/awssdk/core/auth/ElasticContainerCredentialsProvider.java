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

package software.amazon.awssdk.core.auth;

import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.core.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.core.retry.internal.CredentialsEndpointRetryPolicy;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * {@link AwsCredentialsProvider} implementation that loads credentials from the Amazon Elastic Container Service.
 *
 * <p>The URI path is retrieved from the environment variable "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" in the container's
 * environment. If the environment variable is not set, this credentials provider will always return null.</p>
 */
public class ElasticContainerCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ElasticContainerCredentialsProvider.class);

    /**
     * The client to use to fetch the Amazon ECS credentials.
     */
    private final EC2CredentialsProvider credentialsFetcher;

    /**
     * @see #builder()
     */
    private ElasticContainerCredentialsProvider(Builder builder) {
        this.credentialsFetcher = new EC2CredentialsProvider(builder.credentialsEndpointProvider,
                                                             builder.asyncCredentialUpdateEnabled,
                                                             "elastic-container-credentials-provider");
    }

    /**
     * Create an create of this provider using the default configuration. For custom configuration, see {@link #builder()}.
     */
    public static ElasticContainerCredentialsProvider create() {
        return new ElasticContainerCredentialsProvider(builder());
    }

    /**
     * Create a builder for creating a {@link ElasticContainerCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials getCredentials() {
        if (!isEnabled()) {
            throw new SdkClientException(String.format(
                    "Credentials cannot be loaded from ECS because the ECS credentials environment variable (%s) and system "
                    + "property (%s) are not set or cannot be accessed due to the security manager.",
                    AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.environmentVariable(),
                    AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.property()));
        }
        return credentialsFetcher.getCredentials();
    }

    @Override
    public void close() {
        credentialsFetcher.close();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private static boolean isEnabled() {
        return AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue().isPresent();
    }

    /**
     * Retrieve the endpoint that should be used to communicate with the elastic container service to retrieve credentials.
     */
    private static class EcsCredentialsEndpointProvider extends CredentialsEndpointProvider {
        @Override
        public URI getCredentialsEndpoint() throws URISyntaxException {
            return new URI(AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.getStringValueOrThrow() +
                           AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValueOrThrow());
        }

        @Override
        public CredentialsEndpointRetryPolicy getRetryPolicy() {
            return new ContainerCredentialsRetryPolicy();
        }
    }

    /**
     * A builder for creating a custom a {@link ElasticContainerCredentialsProvider}.
     */
    public static final class Builder {
        private Boolean asyncCredentialUpdateEnabled = false;
        private CredentialsEndpointProvider credentialsEndpointProvider = new EcsCredentialsEndpointProvider();

        /**
         * Created using {@link #builder()}.
         */
        private Builder() {}

        /**
         * Configure whether this provider should fetch credentials asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #getCredentials()} is called, but additional resources are used to maintain the
         * provider.
         *
         * <p>By default, this is disabled.</p>
         */
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        @SdkTestInternalApi
        Builder credentialsEndpointProvider(CredentialsEndpointProvider credentialsEndpointProvider) {
            this.credentialsEndpointProvider = credentialsEndpointProvider;
            return this;
        }

        /**
         * Build a {@link ElasticContainerCredentialsProvider} from the provided configuration.
         */
        public ElasticContainerCredentialsProvider build() {
            return new ElasticContainerCredentialsProvider(this);
        }
    }
}
