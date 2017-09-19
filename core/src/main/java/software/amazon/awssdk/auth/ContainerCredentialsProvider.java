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

package software.amazon.awssdk.auth;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.retry.internal.CredentialsEndpointRetryPolicy;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link AwsCredentialsProvider} implementation that loads credentials from the Amazon Elastic Container Service.
 *
 * <p>The URI path is retrieved from the environment variable "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" in the container's
 * environment. If the environment variable is not set, this credentials provider will throw an exception.</p>
 */
public final class ContainerCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ContainerCredentialsProvider.class);

    /**
     * The client to use to fetch the Amazon credentials.
     */
    private final HttpCredentialsProvider credentialsFetcher;
    private final CredentialsEndpointProvider credentialsEndpointProvider;

    /**
     * @see #builder()
     */
    private ContainerCredentialsProvider(Builder builder) {
        this.credentialsEndpointProvider = builder.credentialsEndpointProvider;
        this.credentialsFetcher = new HttpCredentialsProvider(
            Validate.paramNotNull(builder.credentialsEndpointProvider, "credentialsEndpointProvider"),
            builder.asyncCredentialUpdateEnabled,
            "elastic-container-credentials-provider");
    }

    /**
     * Create a builder for creating a {@link ContainerCredentialsProvider}.
     */
    static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials getCredentials() {
        if (!credentialsEndpointProvider.isEnabled()) {
            throw new SdkClientException(credentialsEndpointProvider.disabledExceptionMessage());
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

    /**
     * Retrieve the endpoint that should be used to communicate with the elastic container service to retrieve credentials.
     */
    static class EcsCredentialsEndpointProvider implements CredentialsEndpointProvider {
        @Override
        public URI endpoint() {
            return URI.create(AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.getStringValueOrThrow() +
                           AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValueOrThrow());
        }

        @Override
        public CredentialsEndpointRetryPolicy retryPolicy() {
            return new ContainerCredentialsRetryPolicy();
        }

        @Override
        public boolean isEnabled() {
            return AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue().isPresent();
        }

        @Override
        public String disabledExceptionMessage() {
            return String.format(
                "Credentials cannot be loaded from ECS because the ECS credentials environment variable (%s) and system "
                + "property (%s) are not set or cannot be accessed due to the security manager.",
                AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.environmentVariable(),
                AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.property());
        }
    }

    static class FullUriGenericCredentialsEndpointProvider implements CredentialsEndpointProvider {

        private static Set<String> ALLOWED_HOSTS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("localhost", "127.0.0.1")));

        @Override
        public URI endpoint() throws IOException {
            URI uri = URI.create(AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValueOrThrow());
            if (!ALLOWED_HOSTS.contains(uri.getHost())) {

                throw new SdkClientException(
                    String.format("The full URI (%s) contained withing environment variable %s has an invalid host. "
                                  + "Host can only be one of [%s].",
                                  uri,
                                  AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(),
                                  ALLOWED_HOSTS.stream().collect(joining(","))));
            }
            return uri;
        }

        @Override
        public Map<String, String> headers() {
            return AwsSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN.getStringValue()
                                                                     .filter(StringUtils::isNotBlank)
                                                                     .map(t -> Collections.singletonMap("Authorization", t))
                                                                     .orElseGet(Collections::emptyMap);
        }

        @Override
        public boolean isEnabled() {
            return AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValue().isPresent();
        }

        @Override
        public String disabledExceptionMessage() {
            return String.format(
                "Credentials cannot be loaded from the container because the credentials environment variable (%s) and system "
                + "property (%s) are not set or cannot be accessed due to the security manager.",
                AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(),
                AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.property());
        }
    }

    /**
     * A builder for creating a custom a {@link ContainerCredentialsProvider}.
     */
    static final class Builder {
        private Boolean asyncCredentialUpdateEnabled = false;
        private CredentialsEndpointProvider credentialsEndpointProvider;

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

        public Builder credentialsEndpointProvider(CredentialsEndpointProvider credentialsEndpointProvider) {
            this.credentialsEndpointProvider = credentialsEndpointProvider;
            return this;
        }

        /**
         * Build a {@link ContainerCredentialsProvider} from the provided configuration.
         */
        public ContainerCredentialsProvider build() {
            return new ContainerCredentialsProvider(this);
        }
    }
}
