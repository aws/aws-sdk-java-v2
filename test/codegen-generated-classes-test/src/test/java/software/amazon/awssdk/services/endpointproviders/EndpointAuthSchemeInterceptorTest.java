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

package software.amazon.awssdk.services.endpointproviders;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.protocolquery.model.AllTypesRequest;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.RestJsonEndpointProvidersEndpointAuthSchemeInterceptor;

public class EndpointAuthSchemeInterceptorTest {
    private static final ExecutionInterceptor INTERCEPTOR = new RestJsonEndpointProvidersEndpointAuthSchemeInterceptor();

    @Test
    public void modifyRequest_sigV4Scheme_overridesCorrectSigner() {
        SigV4AuthScheme scheme = SigV4AuthScheme.builder().build();

        SdkRequest request = AllTypesRequest.builder().build();

        Endpoint endpoint = Endpoint.builder()
                                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(scheme))
                                    .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint);

        SdkRequest modified = INTERCEPTOR.modifyRequest(InterceptorContext.builder().request(request).build(), attrs);

        assertThat(modified.overrideConfiguration().flatMap(o -> o.signer()).get()).isInstanceOf(Aws4Signer.class);
    }

    @Test
    public void modifyRequest_sigV4aScheme_overridesCorrectSigner() {
        SigV4aAuthScheme scheme = SigV4aAuthScheme.builder()
                                                  .addSigningRegion("*")
                                                  .build();

        SdkRequest request = AllTypesRequest.builder().build();

        Endpoint endpoint = Endpoint.builder()
                                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(scheme))
                                    .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint);

        // Will throw since Crt is not on the classpath, which is fine for this test.
        assertThatThrownBy(() -> INTERCEPTOR.modifyRequest(InterceptorContext.builder().request(request).build(), attrs))
            .hasMessageContaining("AwsCrtV4aSigner");
    }

    @Test
    public void modifyRequest_signerOverriddenOnRequest_doesNotModify() {
        Signer overrideSigner = mock(Signer.class);

        SigV4AuthScheme scheme = SigV4AuthScheme.builder().build();

        SdkRequest request = AllTypesRequest.builder()
                                            .overrideConfiguration(o -> o.signer(overrideSigner))
                                            .build();

        Endpoint endpoint = Endpoint.builder()
                                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(scheme))
                                    .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint);

        SdkRequest modified = INTERCEPTOR.modifyRequest(InterceptorContext.builder().request(request).build(), attrs);

        assertThat(modified.overrideConfiguration().flatMap(o -> o.signer()).get()).isSameAs(overrideSigner);
    }

    @Test
    public void modifyRequest_signerOverriddenClient_doesNotModify() {
        SigV4AuthScheme scheme = SigV4AuthScheme.builder().build();

        SdkRequest request = AllTypesRequest.builder()
                                            .build();

        Endpoint endpoint = Endpoint.builder()
                                    .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, Arrays.asList(scheme))
                                    .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT, endpoint);
        attrs.putAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN, true);

        SdkRequest modified = INTERCEPTOR.modifyRequest(InterceptorContext.builder().request(request).build(), attrs);

        assertThat(modified.overrideConfiguration().flatMap(o -> o.signer()).orElse(null)).isNull();
    }
}
