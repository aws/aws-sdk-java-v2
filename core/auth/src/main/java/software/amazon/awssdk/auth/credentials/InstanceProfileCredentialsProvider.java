/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.regions.util.HttpResourcesUtils;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;
import software.amazon.awssdk.utils.ToString;

/**
 * Credentials provider implementation that loads credentials from the Amazon EC2 Instance Metadata Service.
 *
 * <P>
 * If {@link SdkSystemSetting#AWS_EC2_METADATA_DISABLED} is set to true, it will not try to load
 * credentials from EC2 metadata service and will return null.
 */
@SdkPublicApi
public final class InstanceProfileCredentialsProvider extends HttpCredentialsProvider {
    private static final String EC2_METADATA_TOKEN_HEADER = "x-aws-ec2-metadata-token";

    private static final String SECURITY_CREDENTIALS_RESOURCE = "/latest/meta-data/iam/security-credentials/";

    /**
     * @see #builder()
     */
    private InstanceProfileCredentialsProvider(BuilderImpl builder) {
        super(builder);
    }

    /**
     * Create a builder for creating a {@link InstanceProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a {@link InstanceProfileCredentialsProvider} with default values.
     *
     * @return a {@link InstanceProfileCredentialsProvider}
     */
    public static InstanceProfileCredentialsProvider create() {
        return builder().build();
    }

    @Override
    protected ResourcesEndpointProvider getCredentialsEndpointProvider() {
        return new InstanceProviderCredentialsEndpointProvider(getToken());
    }

    @Override
    protected boolean isLocalCredentialLoadingDisabled() {
        return SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow();
    }

    @Override
    public String toString() {
        return ToString.create("InstanceProfileCredentialsProvider");
    }

    private String getToken() {
        return EC2MetadataUtils.getToken();
    }

    private static ResourcesEndpointProvider includeTokenHeader(ResourcesEndpointProvider provider, String token) {
        return new ResourcesEndpointProvider() {
            @Override
            public URI endpoint() throws IOException {
                return provider.endpoint();
            }

            @Override
            public Map<String, String> headers() {
                Map<String, String> headers = new HashMap<>(provider.headers());
                headers.put(EC2_METADATA_TOKEN_HEADER, token);
                return headers;
            }
        };
    }

    private static final class InstanceProviderCredentialsEndpointProvider implements ResourcesEndpointProvider {
        private final String metadataToken;

        private InstanceProviderCredentialsEndpointProvider(String metadataToken) {
            this.metadataToken = metadataToken;
        }

        @Override
        public URI endpoint() throws IOException {
            String host = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getStringValueOrThrow();

            URI endpoint = URI.create(host + SECURITY_CREDENTIALS_RESOURCE);
            ResourcesEndpointProvider endpointProvider = () -> endpoint;

            if (metadataToken != null) {
                endpointProvider = includeTokenHeader(endpointProvider, metadataToken);
            }

            String securityCredentialsList = HttpResourcesUtils.instance().readResource(endpointProvider);
            String[] securityCredentials = securityCredentialsList.trim().split("\n");

            if (securityCredentials.length == 0) {
                throw SdkClientException.builder().message("Unable to load credentials path").build();
            }

            return URI.create(host + SECURITY_CREDENTIALS_RESOURCE + securityCredentials[0]);
        }

        @Override
        public Map<String, String> headers() {
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("User-Agent", UserAgentUtils.getUserAgent());
            requestHeaders.put("Accept", "*/*");
            requestHeaders.put("Connection", "keep-alive");

            if (metadataToken != null) {
                requestHeaders.put(EC2_METADATA_TOKEN_HEADER, metadataToken);
            }

            return requestHeaders;
        }
    }


    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<InstanceProfileCredentialsProvider, Builder> {

        /**
         * Build a {@link InstanceProfileCredentialsProvider} from the provided configuration.
         */
        @Override
        InstanceProfileCredentialsProvider build();
    }

    private static final class BuilderImpl
        extends HttpCredentialsProvider.BuilderImpl<InstanceProfileCredentialsProvider, Builder>
        implements Builder {

        private BuilderImpl() {
            super.asyncThreadName("instance-profile-credentials-provider");
        }

        @Override
        public InstanceProfileCredentialsProvider build() {
            return new InstanceProfileCredentialsProvider(this);
        }
    }
}
