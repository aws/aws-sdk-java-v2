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

package software.amazon.awssdk.auth.credentials;

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
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.internal.ContainerCredentialsRetryPolicy;
import software.amazon.awssdk.auth.credentials.internal.HttpCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.internal.util.ResourcesEndpointProvider;
import software.amazon.awssdk.regions.internal.util.ResourcesEndpointRetryPolicy;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;

/**
 * {@link AwsCredentialsProvider} implementation that loads credentials from a local metadata service.
 *
 * Currently supported containers:
 * <ul>
 *     <li>Amazon Elastic Container Service (ECS)</li>
 *     <li>AWS Greengrass</li>
 * </ul>
 *
 * <p>The URI path is retrieved from the environment variable "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" or
 * "AWS_CONTAINER_CREDENTIALS_FULL_URI" in the container's environment. If the environment variable is not set, this credentials
 * provider will throw an exception.</p>
 *
 * @see <a href="http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html">Amazon Elastic Container
 * Service (ECS)</a>
 */
@SdkPublicApi
public final class ContainerCredentialsProvider extends HttpCredentialsProvider {
    private final ResourcesEndpointProvider credentialsEndpointProvider;

    /**
     * @see #builder()
     */
    private ContainerCredentialsProvider(BuilderImpl builder) {
        super(builder);
        this.credentialsEndpointProvider = builder.credentialsEndpointProvider;
    }

    /**
     * Create a builder for creating a {@link ContainerCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    protected ResourcesEndpointProvider getCredentialsEndpointProvider() {
        return credentialsEndpointProvider;
    }

    @Override
    public String toString() {
        return ToString.create("ContainerCredentialsProvider");
    }

    static final class ContainerCredentialsEndpointProvider implements ResourcesEndpointProvider {
        private static final Set<String> ALLOWED_HOSTS = unmodifiableSet(new HashSet<>(Arrays.asList("localhost", "127.0.0.1")));

        @Override
        public URI endpoint() throws IOException {

            if (!SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue().isPresent() &&
                !SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValue().isPresent()) {
                throw SdkClientException.builder()
                        .message(String.format("Cannot fetch credentials from container - neither %s or %s " +
                                 "environment variables are set.",
                                 SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.environmentVariable(),
                                 SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.environmentVariable()))
                        .build();
            }

            try {
                return SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.getStringValue()
                                                                              .map(this::createUri)
                                                                              .orElseGet(this::createGenericContainerUrl);
            } catch (SdkClientException e) {
                throw e;
            } catch (Exception e) {
                throw SdkClientException.builder()
                                        .message("Unable to fetch credentials from container.")
                                        .cause(e)
                                        .build();
            }
        }

        @Override
        public ResourcesEndpointRetryPolicy retryPolicy() {
            return new ContainerCredentialsRetryPolicy();
        }

        @Override
        public Map<String, String> headers() {
            return SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN.getStringValue()
                                                                     .filter(StringUtils::isNotBlank)
                                                                     .map(t -> singletonMap("Authorization", t))
                                                                     .orElseGet(Collections::emptyMap);
        }

        private URI createUri(String relativeUri) {
            return URI.create(SdkSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.getStringValueOrThrow() + relativeUri);
        }

        private URI createGenericContainerUrl() {
            URI uri = URI.create(SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI.getStringValueOrThrow());
            if (!ALLOWED_HOSTS.contains(uri.getHost())) {

                throw SdkClientException.builder()
                                        .message(String.format("The full URI (%s) contained withing environment " +
                                                 "variable %s has an invalid host. Host can only be one of [%s].",
                                                 uri,
                                                 SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_FULL_URI
                                                                 .environmentVariable(),
                                                 ALLOWED_HOSTS.stream().collect(joining(","))))
                                        .build();
            }
            return uri;
        }
    }

    /**
     * A builder for creating a custom a {@link ContainerCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<ContainerCredentialsProvider, Builder> {

        /**
         * Build a {@link ContainerCredentialsProvider} from the provided configuration.
         */
        @Override
        ContainerCredentialsProvider build();
    }

    static final class BuilderImpl extends HttpCredentialsProvider.BuilderImpl<ContainerCredentialsProvider, Builder>
        implements Builder {

        private ResourcesEndpointProvider credentialsEndpointProvider = new ContainerCredentialsEndpointProvider();

        BuilderImpl() {
            super.asyncThreadName("container-credentials-provider");
        }

        @SdkTestInternalApi
        Builder credentialsEndpointProvider(ResourcesEndpointProvider credentialsEndpointProvider) {
            this.credentialsEndpointProvider = credentialsEndpointProvider;
            return this;
        }

        @Override
        public ContainerCredentialsProvider build() {
            return new ContainerCredentialsProvider(this);
        }
    }
}