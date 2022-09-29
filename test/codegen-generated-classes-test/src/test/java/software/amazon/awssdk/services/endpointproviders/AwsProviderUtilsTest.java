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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.rules.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.rules.AwsProviderUtils;
import software.amazon.awssdk.awscore.rules.EndpointAuthScheme;
import software.amazon.awssdk.awscore.rules.SigV4AuthScheme;
import software.amazon.awssdk.awscore.rules.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.rules.Identifier;
import software.amazon.awssdk.core.rules.Value;
import software.amazon.awssdk.core.rules.model.Endpoint;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.MapUtils;

public class AwsProviderUtilsTest {
    @Test
    public void endpointOverridden_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, false);
        assertThat(AwsProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        assertThat(AwsProviderUtils.endpointIsOverridden(attrs)).isTrue();
    }

    @Test
    public void endpointIsDiscovered_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, false);
        assertThat(AwsProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
        assertThat(AwsProviderUtils.endpointIsDiscovered(attrs)).isTrue();
    }

    @Test
    public void valueAsEndpoint_isNone_throws() {
        assertThatThrownBy(() -> AwsProviderUtils.valueAsEndpointOrThrow(Value.none()))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void valueAsEndpoint_isString_throwsAsMsg() {
        assertThatThrownBy(() -> AwsProviderUtils.valueAsEndpointOrThrow(Value.fromStr("oops!")))
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

        assertThat(expected.url()).isEqualTo(AwsProviderUtils.valueAsEndpointOrThrow(endpointVal).url());
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

        assertThat(AwsProviderUtils.valueAsEndpointOrThrow(endpointVal).attribute(AwsEndpointAttribute.AUTH_SCHEMES))
            .containsExactly(sigv4, sigv4a);
    }

    @Test
    public void valueAsEndpoint_endpointHasUnknownProperty_ignores() {
        Value.Endpoint endpointVal = Value.Endpoint.builder()
                                                   .url("https://myservice.aws")
                                                   .property("foo", Value.fromStr("baz"))
                                                   .build();

        assertThat(AwsProviderUtils.valueAsEndpointOrThrow(endpointVal).attribute(AwsEndpointAttribute.AUTH_SCHEMES)).isNull();
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

        assertThat(AwsProviderUtils.valueAsEndpointOrThrow(endpointVal).headers()).isEqualTo(expectedHeaders);
    }

    @Test
    public void regionBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        assertThat(AwsProviderUtils.regionBuiltIn(attrs)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    public void dualStackEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, true);
        assertThat(AwsProviderUtils.dualStackEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void fipsEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, true);
        assertThat(AwsProviderUtils.fipsEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void endpointBuiltIn_doesNotIncludeQueryParams() {
        URI endpoint = URI.create("https://example.com/path?foo=bar");
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        attrs.putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, endpoint);

        assertThat(AwsProviderUtils.endpointBuiltIn(attrs).toString()).isEqualTo("https://example.com/path");
    }

    @Test
    public void useGlobalEndpointBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.USE_GLOBAL_ENDPOINT, true);
        assertThat(AwsProviderUtils.useGlobalEndpointBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void setUri_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI requestUri = URI.create("https://override.example.com/a/c");
        URI resolvedUri = URI.create("https://override.example.com/a/b");

        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(requestUri)
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com/a/b/c");
    }
}
