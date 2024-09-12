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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.internal.DefaultIdentityProviders;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.auth.scheme.internal.DefaultS3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.auth.scheme.internal.S3AuthSchemeInterceptor;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.AttributeMap;

class S3ExpressAuthSchemeProviderTest {

    private static final String S3EXPRESS_BUCKET = "s3expressformat--use1-az1--x-s3";

    @Test
    public void s3express_defaultAuthEnabled_returnspressAuthScheme() {
        PutObjectRequest request = PutObjectRequest.builder().bucket(S3EXPRESS_BUCKET).key("k").build();
        AttributeMap clientContextParams = AttributeMap.builder().build();
        ExecutionAttributes executionAttributes = requiredExecutionAttributes(clientContextParams);

        new S3AuthSchemeInterceptor().beforeExecution(() -> request, executionAttributes);

        SelectedAuthScheme<?> attribute = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(attribute).isNotNull();
        verifyAuthScheme("aws.auth#sigv4-s3express", attribute);
    }

    private ExecutionAttributes requiredExecutionAttributes(AttributeMap clientContextParams) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER, DefaultS3AuthSchemeProvider.create());
        executionAttributes.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "PutObject");
        executionAttributes.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, clientContextParams);
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemesWithS3Express());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS,
                                         DefaultIdentityProviders.builder()
                                                                 .putIdentityProvider(DefaultCredentialsProvider.create())
                                                                 .build());
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                         ClientEndpointProvider.forEndpointOverride(URI.create("https://localhost")));
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER,
                                         S3EndpointProvider.defaultProvider());
        return executionAttributes;
    }

    @Test
    public void s3express_authDisabled_returnsV4AuthScheme() {
        PutObjectRequest request = PutObjectRequest.builder().bucket(S3EXPRESS_BUCKET).key("k").build();
        AttributeMap clientContextParams = AttributeMap.builder()
                                                       .put(S3ClientContextParams.DISABLE_S3_EXPRESS_SESSION_AUTH, true)
                                                       .build();
        ExecutionAttributes executionAttributes = requiredExecutionAttributes(clientContextParams);

        new S3AuthSchemeInterceptor().beforeExecution(() -> request, executionAttributes);

        SelectedAuthScheme<?> attribute = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        assertThat(attribute).isNotNull();
        verifyAuthScheme("aws.auth#sigv4", attribute);
    }

    private void verifyAuthScheme(String expectedAuthSchemeId, SelectedAuthScheme<?> authScheme) {
        assertThat(authScheme).isNotNull();
        assertThat(authScheme.authSchemeOption()).isNotNull();
        assertThat(authScheme.authSchemeOption().schemeId()).isEqualTo(expectedAuthSchemeId);

        assertThat(authScheme.identity()).isNotNull();
        assertThat(authScheme.signer()).isNotNull();
    }

    private Map<String, AuthScheme<?>> authSchemesWithS3Express() {
        Map<String, AuthScheme<?>> schemes = new HashMap<>();
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        schemes.put(awsV4AuthScheme.schemeId(), awsV4AuthScheme);
        S3ExpressAuthScheme s3ExpressAuthScheme = new S3ExpressAuthScheme() {
            @Override
            public IdentityProvider<S3ExpressSessionCredentials> identityProvider(IdentityProviders providers) {
                return new IdentityProvider<S3ExpressSessionCredentials>() {
                    @Override
                    public Class<S3ExpressSessionCredentials> identityType() {
                        return null;
                    }

                    @Override
                    public CompletableFuture<? extends S3ExpressSessionCredentials> resolveIdentity(ResolveIdentityRequest request) {
                        return CompletableFuture.completedFuture(S3ExpressSessionCredentials.create("a","b","c"));
                    }
                };
            }

            @Override
            public HttpSigner<S3ExpressSessionCredentials> signer() {
                return DefaultS3ExpressHttpSigner.create();
            }

            @Override
            public String schemeId() {
                return SCHEME_ID;
            }
        };
        schemes.put(s3ExpressAuthScheme.schemeId(), s3ExpressAuthScheme);
        return schemes;
    }
}