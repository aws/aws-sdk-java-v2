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

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableSet;
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
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.retry.internal.CredentialsEndpointRetryPolicy;
import software.amazon.awssdk.utils.StringUtils;

/**
 * {@link AwsCredentialsProvider} implementation that loads credentials from the Amazon Elastic Container Service.
 *
 * <p>The URI path is retrieved from the environment variable "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" in the container's
 * environment. If the environment variable is not set, this credentials provider will throw an exception.</p>
 */
public final class ContainerCredentialsProvider extends HttpCredentialsProvider {
    private static final Logger log = LoggerFactory.getLogger(ContainerCredentialsProvider.class);
    private final CredentialsEndpointProvider credentialsEndpointProvider;

    /**
     * @see #builder()
     */
    private ContainerCredentialsProvider(Builder builder) {
        super(builder);
        this.credentialsEndpointProvider = builder.credentialsEndpointProvider;
    }

    /**
     * Create a builder for creating a {@link ContainerCredentialsProvider}.
     */
    static Builder builder() {
        return new Builder();
    }

    @Override
    protected CredentialsEndpointProvider getCredentialsEndpointProvider() {
        return credentialsEndpointProvider;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    static final class ContainerCredentialsEndpointProvider implements CredentialsEndpointProvider {
        private static final Set<String> ALLOWED_HOSTS = unmodifiableSet(new HashSet<>(Arrays.asList("localhost", "127.0.0.1")));

        @Override
        public URI endpoint() throws IOException {

            if (!AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue().isPresent() &&
                !AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValue().isPresent()) {
                throw new SdkClientException(
                    String.format("Cannot fetch credentials from container - neither %s or %s environment variables are set.",
                                  AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(),
                                  AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.environmentVariable()));
            }

            try {
                return AwsSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue().map(relativeUri -> {
                    return URI.create(AwsSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.getStringValueOrThrow() + relativeUri);
                }).orElseGet(this::createGenericContainerUrl);
            } catch (SdkClientException e) {
                throw e;
            } catch (Exception e) {
                throw new SdkClientException("Unable to fetch credentials from container.", e);
            }
        }

        @Override
        public CredentialsEndpointRetryPolicy retryPolicy() {
            return new ContainerCredentialsRetryPolicy();
        }

        @Override
        public Map<String, String> headers() {
            return AwsSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN.getStringValue()
                                                                     .filter(StringUtils::isNotBlank)
                                                                     .map(t -> singletonMap("Authorization", t))
                                                                     .orElseGet(Collections::emptyMap);
        }

        private URI createGenericContainerUrl() {
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
    }

    /**
     * A builder for creating a custom a {@link ContainerCredentialsProvider}.
     */
    static final class Builder extends HttpCredentialsProvider.Builder<ContainerCredentialsProvider, Builder> {

        private CredentialsEndpointProvider credentialsEndpointProvider = new ContainerCredentialsEndpointProvider();

        /**
         * Created using {@link #builder()}.
         */
        private Builder() {
            super.asyncThreadName("container-credentials-provider");
        }

        @SdkTestInternalApi
        Builder credentialsEndpointProvider(CredentialsEndpointProvider credentialsEndpointProvider) {
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
