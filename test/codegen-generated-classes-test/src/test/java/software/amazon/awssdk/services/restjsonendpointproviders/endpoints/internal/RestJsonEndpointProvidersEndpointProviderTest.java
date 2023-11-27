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

package software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.MapUtils;

class RestJsonEndpointProvidersEndpointProviderTest {

    private DefaultRestJsonEndpointProvidersEndpointProvider provider;

    @BeforeEach
    void init() {
        this.provider = new DefaultRestJsonEndpointProvidersEndpointProvider();
    }

    @Test
    public void valueAsEndpoint_isNone_throws() {
        assertThatThrownBy(() -> provider.valueAsEndpointOrThrow(Value.none()))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void valueAsEndpoint_isString_throwsAsMsg() {
        assertThatThrownBy(() -> provider.valueAsEndpointOrThrow(Value.fromStr("oops!")))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("oops!");
    }

    @Test
    public void valueAsEndpoint_isEndpoint_returnsEndpoint() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .build();

        Endpoint expected = Endpoint.builder()
                                    .url(URI.create("https://myservice.aws"))
                                    .build();

        assertThat(expected.url()).isEqualTo(provider.valueAsEndpointOrThrow(endpointVal).url());
    }

    @Test
    public void valueAsEndpoint_endpointHasAuthSchemes_includesAuthSchemes() {
        List<Value> authSchemes = Arrays.asList(
            Value.fromRecord(MapUtils.of(Identifier.of("name"), Value.fromStr("sigv4"),
                                         Identifier.of("signingRegion"), Value.fromStr("us-west-2"),
                                         Identifier.of("signingName"), Value.fromStr("myservice"),
                                         Identifier.of("disableDoubleEncoding"), Value.fromBool(false))),

            Value.fromRecord(MapUtils.of(Identifier.of("name"), Value.fromStr("sigv4a"),
                                         Identifier.of("signingRegionSet"),
                                         Value.fromArray(Collections.singletonList(Value.fromStr("*"))),
                                         Identifier.of("signingName"), Value.fromStr("myservice"),
                                         Identifier.of("disableDoubleEncoding"), Value.fromBool(false))),

            // Unknown scheme name, should ignore
            Value.fromRecord(MapUtils.of(Identifier.of("name"), Value.fromStr("sigv5")))
        );


        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .property("authSchemes", Value.fromArray(authSchemes))
                                                   .build();


        EndpointAuthScheme sigv4 = SigV4AuthScheme.builder()
                                                  .signingName("myservice")
                                                  .signingRegion("us-west-2")
                                                  .disableDoubleEncoding(false)
                                                  .build();

        EndpointAuthScheme sigv4a = SigV4aAuthScheme.builder()
                                                    .signingName("myservice")
                                                    .addSigningRegion("*")
                                                    .disableDoubleEncoding(false)
                                                    .build();

        assertThat(provider.valueAsEndpointOrThrow(endpointVal).attribute(AwsEndpointAttribute.AUTH_SCHEMES))
            .containsExactly(sigv4, sigv4a);
    }

    @Test
    public void valueAsEndpoint_endpointHasUnknownProperty_ignores() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .property("foo", Value.fromStr("baz"))
                                                   .build();

        assertThat(provider.valueAsEndpointOrThrow(endpointVal).attribute(AwsEndpointAttribute.AUTH_SCHEMES)).isNull();
    }

    @Test
    public void valueAsEndpoint_endpointHasHeaders_includesHeaders() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .addHeader("foo1", "bar1")
                                                   .addHeader("foo1", "bar2")
                                                   .addHeader("foo2", "baz")
                                                   .build();

        Map<String, List<String>> expectedHeaders = MapUtils.of("foo1", Arrays.asList("bar1", "bar2"),
                                                                "foo2", Arrays.asList("baz"));

        assertThat(provider.valueAsEndpointOrThrow(endpointVal).headers()).isEqualTo(expectedHeaders);
    }

}