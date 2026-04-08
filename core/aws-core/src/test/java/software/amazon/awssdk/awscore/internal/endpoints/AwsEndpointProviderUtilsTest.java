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

package software.amazon.awssdk.awscore.internal.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;

class AwsEndpointProviderUtilsTest {

    @Test
    void regionBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        assertThat(AwsEndpointProviderUtils.regionBuiltIn(attrs)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    void dualStackEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, true);
        assertThat(AwsEndpointProviderUtils.dualStackEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    void fipsEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, true);
        assertThat(AwsEndpointProviderUtils.fipsEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    void endpointBuiltIn_doesNotIncludeQueryParams() {
        URI endpoint = URI.create("https://example.com/path?foo=bar");
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                           ClientEndpointProvider.forEndpointOverride(endpoint));
        attrs.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, new BusinessMetricCollection());

        assertThat(AwsEndpointProviderUtils.endpointBuiltIn(attrs)).isEqualTo("https://example.com/path");
    }

    @Test
    void endpointBuiltIn_notOverridden_returnsNull() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, endpointProvider(false));
        assertThat(AwsEndpointProviderUtils.endpointBuiltIn(attrs)).isNull();
    }

    @Test
    void endpointOverridden_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, endpointProvider(false));
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    void endpointOverridden_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, endpointProvider(true));
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isTrue();
    }

    @Test
    void endpointIsDiscovered_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, false);
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    void endpointIsDiscovered_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    void endpointIsDiscovered_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isTrue();
    }

    @Test
    void disableHostPrefixInjection_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION, false);
        assertThat(AwsEndpointProviderUtils.disableHostPrefixInjection(attrs)).isFalse();
    }

    @Test
    void disableHostPrefixInjection_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.disableHostPrefixInjection(attrs)).isFalse();
    }

    @Test
    void disableHostPrefixInjection_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION, true);
        assertThat(AwsEndpointProviderUtils.disableHostPrefixInjection(attrs)).isTrue();
    }

    @Test
    void addHostPrefix_prefixIsNull_returnsUnmodified() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder().url(url).build();
        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, null).url()).isEqualTo(url);
    }

    @Test
    void addHostPrefix_prefixIsEmpty_returnsUnmodified() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder().url(url).build();
        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, "").url()).isEqualTo(url);
    }

    @Test
    void addHostPrefix_prefixPresent_returnsPrefixPrepended() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder().url(url).build();
        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, "api.").url())
            .isEqualTo(URI.create("https://api.foo.aws"));
    }

    @Test
    void addHostPrefix_prefixPresent_preservesPortPathAndQuery() {
        URI url = URI.create("https://foo.aws:1234/a/b/c?queryParam1=val1");
        Endpoint e = Endpoint.builder().url(url).build();
        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, "api.").url())
            .isEqualTo(URI.create("https://api.foo.aws:1234/a/b/c?queryParam1=val1"));
    }

    @Test
    void addHostPrefix_prefixInvalid_throws() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder().url(url).build();
        assertThatThrownBy(() -> AwsEndpointProviderUtils.addHostPrefix(e, "foo#bar.*baz"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("component must match the pattern");
    }

    @Test
    void setUri_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI resolvedUri = URI.create("https://override.example.com/a/b");
        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://override.example.com/a/c"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com/a/b/c");
    }

    @Test
    void setUri_doubleSlash_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI resolvedUri = URI.create("https://override.example.com/a/b");
        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://override.example.com/a//c"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com/a/b//c");
    }

    @Test
    void setUri_withTrailingSlashNoPath_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/");
        URI resolvedUri = URI.create("https://override.example.com/");
        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://override.example.com//a"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com//a");
    }

    @Test
    void setUri_noPathDifference_keepsRequestPath() {
        URI clientEndpoint = URI.create("https://example.com");
        URI resolvedUri = URI.create("https://resolved.example.com");
        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://example.com/operation"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        SdkHttpRequest result = AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri);
        assertThat(result.host()).isEqualTo("resolved.example.com");
        assertThat(result.encodedPath()).isEqualTo("/operation");
    }

    private static ClientEndpointProvider endpointProvider(boolean isEndpointOverridden) {
        return ClientEndpointProvider.create(URI.create("https://foo.aws"), isEndpointOverridden);
    }
}
