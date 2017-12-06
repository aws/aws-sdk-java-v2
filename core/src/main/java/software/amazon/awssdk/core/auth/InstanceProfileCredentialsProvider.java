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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.core.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.core.internal.EC2CredentialsUtils;
import software.amazon.awssdk.core.util.EC2MetadataUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Credentials provider implementation that loads credentials from the Amazon EC2 Instance Metadata Service.
 */
public class InstanceProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    /**
     * The client to use to fetch the Amazon ECS credentials.
     */
    private final EC2CredentialsProvider credentialsFetcher;

    /**
     * @see #builder()
     */
    private InstanceProfileCredentialsProvider(Builder builder) {
        this.credentialsFetcher = new EC2CredentialsProvider(new InstanceMetadataCredentialsEndpointProvider(),
                                                             builder.asyncCredentialUpdateEnabled,
                                                             "instance-profile-credentials-provider");
    }

    /**
     * Create an {@link InstanceProfileCredentialsProvider} using the default configuration. See {@link #builder()} for
     * customizing the configuration.
     */
    public static InstanceProfileCredentialsProvider create() {
        return new InstanceProfileCredentialsProvider(builder());
    }

    /**
     * Create a builder for creating a {@link InstanceProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials getCredentials() {
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

    private static class InstanceMetadataCredentialsEndpointProvider extends CredentialsEndpointProvider {
        @Override
        public URI getCredentialsEndpoint() throws URISyntaxException, IOException {
            String host = EC2MetadataUtils.getHostAddressForEc2MetadataService();

            URI endpoint = new URI(host + EC2MetadataUtils.SECURITY_CREDENTIALS_RESOURCE);
            String securityCredentialsList = EC2CredentialsUtils.getInstance().readResource(endpoint);
            String[] securityCredentials = securityCredentialsList.trim().split("\n");

            if (securityCredentials.length == 0) {
                throw new SdkClientException("Unable to load credentials path");
            }

            return new URI(host + EC2MetadataUtils.SECURITY_CREDENTIALS_RESOURCE + securityCredentials[0]);
        }
    }

    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    public static final class Builder {
        private Boolean asyncCredentialUpdateEnabled = false;
        
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

        /**
         * Build a {@link InstanceProfileCredentialsProvider} from the provided configuration.
         */
        public InstanceProfileCredentialsProvider build() {
            return new InstanceProfileCredentialsProvider(this);
        }
    }
}
