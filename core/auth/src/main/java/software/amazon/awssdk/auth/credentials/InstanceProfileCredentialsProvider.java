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
import software.amazon.awssdk.auth.credentials.internal.Ec2MetadataConfigProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.util.SdkUserAgent;
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

    private final String endpoint;
    private final Ec2MetadataConfigProvider configProvider = Ec2MetadataConfigProvider.builder().build();

    /**
     * @see #builder()
     */
    private InstanceProfileCredentialsProvider(BuilderImpl builder) {
        super(builder);
        this.endpoint = builder.endpoint;
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
        return new InstanceProviderCredentialsEndpointProvider(getImdsEndpoint(), getToken());
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
        try {
            return HttpResourcesUtils.instance()
                    .readResource(new TokenEndpointProvider(getImdsEndpoint()), "PUT");
        } catch (Exception e) {

            boolean is400ServiceException = e instanceof SdkServiceException
                    && ((SdkServiceException) e).statusCode() == 400;

            // metadata resolution must not continue to the token-less flow for a 400
            if (is400ServiceException) {
                throw SdkClientException.builder()
                        .message("Unable to fetch metadata token")
                        .cause(e)
                        .build();
            }

            return null;
        }
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

    private String getImdsEndpoint() {
        if (endpoint != null) {
            return endpoint;
        }

        return configProvider.getEndpoint();
    }

    private static final class InstanceProviderCredentialsEndpointProvider implements ResourcesEndpointProvider {
        private final String imdsEndpoint;
        private final String metadataToken;

        private InstanceProviderCredentialsEndpointProvider(String imdsEndpoint, String metadataToken) {
            this.imdsEndpoint = imdsEndpoint;
            this.metadataToken = metadataToken;
        }

        @Override
        public URI endpoint() throws IOException {
            URI endpoint = URI.create(imdsEndpoint + SECURITY_CREDENTIALS_RESOURCE);
            ResourcesEndpointProvider endpointProvider = () -> endpoint;

            if (metadataToken != null) {
                endpointProvider = includeTokenHeader(endpointProvider, metadataToken);
            }

            String securityCredentialsList = HttpResourcesUtils.instance().readResource(endpointProvider);
            String[] securityCredentials = securityCredentialsList.trim().split("\n");

            if (securityCredentials.length == 0) {
                throw SdkClientException.builder().message("Unable to load credentials path").build();
            }

            return URI.create(imdsEndpoint + SECURITY_CREDENTIALS_RESOURCE + securityCredentials[0]);
        }

        @Override
        public Map<String, String> headers() {
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("User-Agent", SdkUserAgent.create().userAgent());
            requestHeaders.put("Accept", "*/*");
            requestHeaders.put("Connection", "keep-alive");

            if (metadataToken != null) {
                requestHeaders.put(EC2_METADATA_TOKEN_HEADER, metadataToken);
            }

            return requestHeaders;
        }
    }

    private static final class TokenEndpointProvider implements ResourcesEndpointProvider {
        private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
        private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
        private static final String DEFAULT_TOKEN_TTL = "21600";

        private final String host;

        private TokenEndpointProvider(String host) {
            this.host = host;
        }

        @Override
        public URI endpoint() {
            String finalHost = host;
            if (finalHost.endsWith("/")) {
                finalHost = finalHost.substring(0, finalHost.length() - 1);
            }
            return URI.create(finalHost + TOKEN_RESOURCE_PATH);
        }

        @Override
        public Map<String, String> headers() {
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("User-Agent", SdkUserAgent.create().userAgent());
            requestHeaders.put("Accept", "*/*");
            requestHeaders.put("Connection", "keep-alive");
            requestHeaders.put(EC2_METADATA_TOKEN_TTL_HEADER, DEFAULT_TOKEN_TTL);

            return requestHeaders;
        }
    }


    /**
     * A builder for creating a custom a {@link InstanceProfileCredentialsProvider}.
     */
    public interface Builder extends HttpCredentialsProvider.Builder<InstanceProfileCredentialsProvider, Builder> {

        Builder endpoint(String endpoint);

        /**
         * Build a {@link InstanceProfileCredentialsProvider} from the provided configuration.
         */
        @Override
        InstanceProfileCredentialsProvider build();
    }

    private static final class BuilderImpl
        extends HttpCredentialsProvider.BuilderImpl<InstanceProfileCredentialsProvider, Builder>
        implements Builder {

        private String endpoint;

        private BuilderImpl() {
            super.asyncThreadName("instance-profile-credentials-provider");
        }

        @Override
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        @Override
        public InstanceProfileCredentialsProvider build() {
            return new InstanceProfileCredentialsProvider(this);
        }
    }
}
