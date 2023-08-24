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
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.AuthSchemeUtils;

public class AuthSchemeUtilsTest {
    @Test
    public void chooseAuthScheme_noSchemesInList_throws() {
        assertThatThrownBy(() -> AuthSchemeUtils.chooseAuthScheme(Collections.emptyList()))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Endpoint did not contain any known auth schemes");
    }

    @Test
    public void chooseAuthScheme_noKnownSchemes_throws() {
        EndpointAuthScheme sigv1 = mock(EndpointAuthScheme.class);
        when(sigv1.name()).thenReturn("sigv1");

        EndpointAuthScheme sigv5 = mock(EndpointAuthScheme.class);
        when(sigv5.name()).thenReturn("sigv5");

        assertThatThrownBy(() -> AuthSchemeUtils.chooseAuthScheme(Arrays.asList(sigv1, sigv5)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Endpoint did not contain any known auth schemes");
    }

    @Test
    public void chooseAuthScheme_multipleSchemesKnown_choosesFirst() {
        EndpointAuthScheme sigv4 = SigV4AuthScheme.builder().build();
        EndpointAuthScheme sigv4a = SigV4aAuthScheme.builder().build();

        assertThat(AuthSchemeUtils.chooseAuthScheme(Arrays.asList(sigv4, sigv4a))).isEqualTo(sigv4);
    }

    @Test
    public void setSigningParams_sigv4_setsParamsCorrectly() {
        AuthSchemeOption sigv4 = AuthSchemeOption.builder()
                                                 .schemeId(AwsV4AuthScheme.SCHEME_ID)
                                                 .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "myservice")
                                                 .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                                                 .putSignerProperty(AwsV4HttpSigner.REGION_NAME, "us-west-2")
                                                 .build();

        ExecutionAttributes attrs = new ExecutionAttributes();

        AuthSchemeUtils.setSigningParams(attrs, sigv4);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(Region.of("us-west-2"));
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void setSigningParams_sigv4_paramsAreNull_doesNotOverrideAttrs() {
        AuthSchemeOption sigv4 = AuthSchemeOption.builder()
                                                 .schemeId(AwsV4AuthScheme.SCHEME_ID)
                                                 .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "myservice");
        attrs.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, Region.of("us-west-2"));
        attrs.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, true);

        AuthSchemeUtils.setSigningParams(attrs, sigv4);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)).isEqualTo(Region.of("us-west-2"));
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }

    @Test
    public void setSigningParams_sigv4a_setsParamsCorrectly() {
        AuthSchemeOption sigv4a = AuthSchemeOption.builder()
                                                  .schemeId(AwsV4aAuthScheme.SCHEME_ID)
                                                  .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "myservice")
                                                  .putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, false)
                                                  .putSignerProperty(AwsV4aHttpSigner.REGION_NAME, "*")
                                                  .build();

        ExecutionAttributes attrs = new ExecutionAttributes();

        AuthSchemeUtils.setSigningParams(attrs, sigv4a);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE)).isEqualTo(RegionScope.GLOBAL);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(false);
    }

    @Test
    public void setSigningParams_sigv4a_signingNameNull_doesNotOverrideAttrs() {
        AuthSchemeOption sigv4a = AuthSchemeOption.builder()
                                                  .schemeId(AwsV4aAuthScheme.SCHEME_ID)
                                                  .putSignerProperty(AwsV4aHttpSigner.REGION_NAME, "*")
                                                  .build();

        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, "myservice");
        attrs.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, true);

        AuthSchemeUtils.setSigningParams(attrs, sigv4a);

        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME)).isEqualTo("myservice");
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE)).isEqualTo(RegionScope.GLOBAL);
        assertThat(attrs.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE)).isEqualTo(true);
    }
}
