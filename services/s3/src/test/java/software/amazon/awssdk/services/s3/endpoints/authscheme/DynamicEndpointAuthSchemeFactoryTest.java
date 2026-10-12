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

package software.amazon.awssdk.services.s3.endpoints.authscheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Tests for {@link DynamicEndpointAuthSchemeFactory}.
 *
 * <p>The generated S3 endpoint provider calls this factory as
 * {@code DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(..).signingName(..).signingRegion(..).create(name)},
 * where {@code name} is resolved at runtime to either {@code sigv4} or {@code sigv4-s3express}. These tests pin that contract.
 */
class DynamicEndpointAuthSchemeFactoryTest {

    @Test
    void create_sigv4_returnsSigV4AuthSchemeWithPropertiesApplied() {
        EndpointAuthScheme scheme = DynamicEndpointAuthSchemeFactory.builder()
                                                                    .disableDoubleEncoding(true)
                                                                    .signingName("s3")
                                                                    .signingRegion("us-west-2")
                                                                    .create("sigv4");

        assertThat(scheme).isInstanceOf(SigV4AuthScheme.class);
        SigV4AuthScheme sigv4 = (SigV4AuthScheme) scheme;
        assertThat(sigv4.name()).isEqualTo("sigv4");
        assertThat(sigv4.schemeId()).isEqualTo("aws.auth#sigv4");
        assertThat(sigv4.signingName()).isEqualTo("s3");
        assertThat(sigv4.signingRegion()).isEqualTo("us-west-2");
        assertThat(sigv4.disableDoubleEncoding()).isTrue();
        assertThat(sigv4.isDisableDoubleEncodingSet()).isTrue();
    }

    @Test
    void create_s3Express_returnsS3ExpressAuthSchemeWithPropertiesApplied() {
        EndpointAuthScheme scheme = DynamicEndpointAuthSchemeFactory.builder()
                                                                    .disableDoubleEncoding(true)
                                                                    .signingName("s3express")
                                                                    .signingRegion("us-west-2")
                                                                    .create("sigv4-s3express");

        assertThat(scheme).isInstanceOf(S3ExpressEndpointAuthScheme.class);
        S3ExpressEndpointAuthScheme s3Express = (S3ExpressEndpointAuthScheme) scheme;
        assertThat(s3Express.name()).isEqualTo("sigv4-s3express");
        assertThat(s3Express.schemeId()).isEqualTo("aws.auth#sigv4-s3express");
        assertThat(s3Express.signingName()).isEqualTo("s3express");
        assertThat(s3Express.signingRegion()).isEqualTo("us-west-2");
        assertThat(s3Express.disableDoubleEncoding()).isTrue();
        assertThat(s3Express.isDisableDoubleEncodingSet()).isTrue();
    }

    /**
     * The scheme name is the only thing that varies at runtime; the collected properties must be applied identically to
     * whichever type is selected. This mirrors the merged S3 ruleset result, where both branches carry the same properties.
     */
    @Test
    void create_sameFactoryProperties_appliedIdenticallyToBothSchemes() {
        DynamicEndpointAuthSchemeFactory factory = DynamicEndpointAuthSchemeFactory.builder()
                                                                                   .disableDoubleEncoding(false)
                                                                                   .signingName("s3express")
                                                                                   .signingRegion("eu-central-1");

        SigV4AuthScheme sigv4 = (SigV4AuthScheme) factory.create("sigv4");
        S3ExpressEndpointAuthScheme s3Express = (S3ExpressEndpointAuthScheme) factory.create("sigv4-s3express");

        assertThat(sigv4.signingName()).isEqualTo(s3Express.signingName());
        assertThat(sigv4.signingRegion()).isEqualTo(s3Express.signingRegion());
        assertThat(sigv4.disableDoubleEncoding()).isEqualTo(s3Express.disableDoubleEncoding());
        assertThat(sigv4.disableDoubleEncoding()).isFalse();
    }

    /**
     * Codegen emits properties in ruleset order, which is not guaranteed to match the declaration order here, so every setter
     * must be order independent and return the same instance.
     */
    @Test
    void setters_calledInAnyOrder_returnSameInstanceAndApplyAllProperties() {
        DynamicEndpointAuthSchemeFactory factory = DynamicEndpointAuthSchemeFactory.builder();

        assertThat(factory.signingRegion("us-east-1")).isSameAs(factory);
        assertThat(factory.disableDoubleEncoding(true)).isSameAs(factory);
        assertThat(factory.signingName("s3")).isSameAs(factory);

        SigV4AuthScheme scheme = (SigV4AuthScheme) factory.create("sigv4");
        assertThat(scheme.signingName()).isEqualTo("s3");
        assertThat(scheme.signingRegion()).isEqualTo("us-east-1");
        assertThat(scheme.disableDoubleEncoding()).isTrue();
    }

    /**
     * Unset properties must behave exactly as they do when codegen emits a direct builder call with those properties omitted,
     * that is, {@code null} rather than a defaulted value.
     */
    @Test
    void create_withNoPropertiesSet_leavesPropertiesUnset() {
        SigV4AuthScheme sigv4 = (SigV4AuthScheme) DynamicEndpointAuthSchemeFactory.builder().create("sigv4");

        assertThat(sigv4.signingName()).isNull();
        assertThat(sigv4.signingRegion()).isNull();
        assertThat(sigv4.isDisableDoubleEncodingSet()).isFalse();
        assertThat(sigv4.disableDoubleEncoding()).isFalse();

        S3ExpressEndpointAuthScheme s3Express =
            (S3ExpressEndpointAuthScheme) DynamicEndpointAuthSchemeFactory.builder().create("sigv4-s3express");

        assertThat(s3Express.signingName()).isNull();
        assertThat(s3Express.signingRegion()).isNull();
        assertThat(s3Express.isDisableDoubleEncodingSet()).isFalse();
        assertThat(s3Express.disableDoubleEncoding()).isFalse();
    }

    @Test
    void create_explicitNullDisableDoubleEncoding_leavesPropertyUnset() {
        SigV4AuthScheme sigv4 = (SigV4AuthScheme) DynamicEndpointAuthSchemeFactory.builder()
                                                                                  .disableDoubleEncoding(null)
                                                                                  .create("sigv4");

        assertThat(sigv4.isDisableDoubleEncodingSet()).isFalse();
        assertThat(sigv4.disableDoubleEncoding()).isFalse();
    }

    /**
     * Anything other than the two names the S3 ruleset can produce is a programming error and must fail fast with a message
     * naming the offending value, rather than silently resolving to the wrong signer.
     */
    @ParameterizedTest
    @ValueSource(strings = {"sigv4a", "sigv4-s3", "SIGV4", "Sigv4-S3Express", "", " ", "bearer"})
    void create_unsupportedName_throwsSdkClientException(String name) {
        assertThatThrownBy(() -> DynamicEndpointAuthSchemeFactory.builder().create(name))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unsupported dynamic endpoint auth scheme name")
            .hasMessageContaining(name)
            .hasMessageContaining("sigv4")
            .hasMessageContaining("sigv4-s3express");
    }

    /**
     * A null name must surface the same actionable error as any other unsupported value, not a NullPointerException.
     */
    @Test
    void create_nullName_throwsSdkClientExceptionNotNpe() {
        assertThatThrownBy(() -> DynamicEndpointAuthSchemeFactory.builder().create(null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unsupported dynamic endpoint auth scheme name");
    }

    /**
     * The factory is reusable: {@code create} must not consume or mutate the collected properties, since a single resolution can
     * in principle build more than one scheme.
     */
    @Test
    void create_calledRepeatedly_returnsEqualButDistinctInstances() {
        DynamicEndpointAuthSchemeFactory factory = DynamicEndpointAuthSchemeFactory.builder()
                                                                                   .signingName("s3express")
                                                                                   .signingRegion("us-west-2")
                                                                                   .disableDoubleEncoding(true);

        EndpointAuthScheme first = factory.create("sigv4-s3express");
        EndpointAuthScheme second = factory.create("sigv4-s3express");

        assertThat(first).isNotSameAs(second);
        assertThat(first).isEqualTo(second);
    }
}
