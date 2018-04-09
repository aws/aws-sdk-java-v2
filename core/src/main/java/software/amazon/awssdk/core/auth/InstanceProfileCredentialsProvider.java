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

package software.amazon.awssdk.core.auth;

import java.io.IOException;
import java.net.URI;
import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.CredentialsEndpointProvider;
import software.amazon.awssdk.core.internal.HttpCredentialsUtils;
import software.amazon.awssdk.utils.ToString;

/**
 * Credentials provider implementation that loads credentials from the Amazon EC2 Instance Metadata Service.
 */
public class InstanceProfileCredentialsProvider extends HttpCredentialsProvider {

    //TODO: make this private
    static final String SECURITY_CREDENTIALS_RESOURCE = "/latest/meta-data/iam/security-credentials/";
    private final CredentialsEndpointProvider credentialsEndpointProvider = new InstanceProviderCredentialsEndpointProvider();

    /**
     * @see #builder()
     */
    private InstanceProfileCredentialsProvider(Builder builder) {
        super(builder);
    }

    /**
     * Create a builder for creating a {@link InstanceProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a {@link InstanceProfileCredentialsProvider} with default values.
     * @return a {@link InstanceProfileCredentialsProvider}
     */
    public static InstanceProfileCredentialsProvider create() {
        return builder().build();
    }

    @Override
    protected CredentialsEndpointProvider getCredentialsEndpointProvider() {
        return credentialsEndpointProvider;
    }

    @Override
    public String toString() {
        return ToString.create("InstanceProfileCredentialsProvider");
    }

    private static class InstanceProviderCredentialsEndpointProvider implements CredentialsEndpointProvider {
        @Override
        public URI endpoint() throws IOException {
            String host = AwsSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getStringValueOrThrow();

            URI endpoint = URI.create(host + SECURITY_CREDENTIALS_RESOURCE);
            String securityCredentialsList = HttpCredentialsUtils.instance().readResource(endpoint);
            String[] securityCredentials = securityCredentialsList.trim().split("\n");

            if (securityCredentials.length == 0) {
                throw new SdkClientException("Unable to load credentials path");
            }

            return URI.create(host + SECURITY_CREDENTIALS_RESOURCE + securityCredentials[0]);
        }
    }

    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    public static final class Builder extends HttpCredentialsProvider.Builder<InstanceProfileCredentialsProvider, Builder> {
        /**
         * Created using {@link #builder()}.
         */
        private Builder() {
            super.asyncThreadName("instance-profile-credentials-provider");
        }

        /**
         * Build a {@link InstanceProfileCredentialsProvider} from the provided configuration.
         */
        public InstanceProfileCredentialsProvider build() {
            return new InstanceProfileCredentialsProvider(this);
        }
    }
}
