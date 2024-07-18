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
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.AwsEndpointProviderUtils;

public class AwsEndpointProviderUtilsTest {
    @Test
    public void endpointOverridden_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, false);
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isTrue();
    }

    @Test
    public void endpointIsDiscovered_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, false);
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isFalse();
    }

    @Test
    public void endpointIsDiscovered_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT, true);
        assertThat(AwsEndpointProviderUtils.endpointIsDiscovered(attrs)).isTrue();
    }

    @Test
    public void disableHostPrefixInjection_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION, false);
        assertThat(AwsEndpointProviderUtils.disableHostPrefixInjection(attrs)).isFalse();
    }

    @Test
    public void disableHostPrefixInjection_attrIsAbsent_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        assertThat(AwsEndpointProviderUtils.disableHostPrefixInjection(attrs)).isFalse();
    }

    @Test
    public void disableHostPrefixInjection_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION, true);
        assertThat(AwsEndpointProviderUtils.disableHostPrefixInjection(attrs)).isTrue();
    }

    @Test
    public void regionBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        assertThat(AwsEndpointProviderUtils.regionBuiltIn(attrs)).isEqualTo(Region.US_EAST_1);
    }

    @Test
    public void dualStackEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, true);
        assertThat(AwsEndpointProviderUtils.dualStackEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void fipsEnabledBuiltIn_returnsAttrValue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, true);
        assertThat(AwsEndpointProviderUtils.fipsEnabledBuiltIn(attrs)).isEqualTo(true);
    }

    @Test
    public void endpointBuiltIn_doesNotIncludeQueryParams() {
        URI endpoint = URI.create("https://example.com/path?foo=bar");
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        attrs.putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, endpoint);

        assertThat(AwsEndpointProviderUtils.endpointBuiltIn(attrs).toString()).isEqualTo("https://example.com/path");
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

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com/a/b/c");
    }

    @Test
    public void setUri_doubleSlash_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI requestUri = URI.create("https://override.example.com/a//c");
        URI resolvedUri = URI.create("https://override.example.com/a/b");

        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(requestUri)
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com/a/b//c");
    }

    @Test
    public void setUri_withTrailingSlashNoPath_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/");
        URI requestUri = URI.create("https://override.example.com//a");
        URI resolvedUri = URI.create("https://override.example.com/");
        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(requestUri)
                                               .method(SdkHttpMethod.GET)
                                               .build();

        assertThat(AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri).getUri().toString())
            .isEqualTo("https://override.example.com//a");
    }

    @Test
    public void addHostPrefix_prefixIsNull_returnsUnModified() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder()
            .url(url)
            .build();

        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, null).url()).isEqualTo(url);
    }

    @Test
    public void addHostPrefix_prefixIsEmpty_returnsUnModified() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder()
                             .url(url)
                             .build();

        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, "").url()).isEqualTo(url);
    }

    @Test
    public void addHostPrefix_prefixPresent_returnsPrefixPrepended() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder()
                             .url(url)
                             .build();

        URI expected = URI.create("https://api.foo.aws");
        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, "api.").url()).isEqualTo(expected);
    }

    @Test
    public void addHostPrefix_prefixPresent_preservesPortPathAndQuery() {
        URI url = URI.create("https://foo.aws:1234/a/b/c?queryParam1=val1");
        Endpoint e = Endpoint.builder()
                             .url(url)
                             .build();

        URI expected = URI.create("https://api.foo.aws:1234/a/b/c?queryParam1=val1");
        assertThat(AwsEndpointProviderUtils.addHostPrefix(e, "api.").url()).isEqualTo(expected);
    }

    @Test
    public void addHostPrefix_prefixInvalid_throws() {
        URI url = URI.create("https://foo.aws");
        Endpoint e = Endpoint.builder()
                             .url(url)
                             .build();

        assertThatThrownBy(() -> AwsEndpointProviderUtils.addHostPrefix(e, "foo#bar.*baz"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("component must match the pattern");
    }
}
