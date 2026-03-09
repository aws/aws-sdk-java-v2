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
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.internal.AwsEndpointProviderUtils;

public class AwsEndpointProviderUtilsTest {
    @Test
    public void endpointOverridden_attrIsFalse_returnsFalse() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, endpointProvider(false));
        assertThat(AwsEndpointProviderUtils.endpointIsOverridden(attrs)).isFalse();
    }

    @Test
    public void endpointOverridden_attrIsTrue_returnsTrue() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, endpointProvider(true));
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
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                           ClientEndpointProvider.forEndpointOverride(endpoint));
        attrs.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, new BusinessMetricCollection());

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

    // --- Property 8: setUri equivalence (EndpointUrl overload vs URI overload) ---
    // Validates: Requirements 5.1, 5.2, 5.3

    @Test
    public void setUri_endpointUrlOverload_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI resolvedUri = URI.create("https://override.example.com/a/b");

        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://override.example.com/a/c"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        SdkHttpRequest uriResult = AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri);
        SdkHttpRequest endpointUrlResult = AwsEndpointProviderUtils.setUri(request, clientEndpoint,
                                                                           EndpointUrl.parse(resolvedUri.toString()));

        assertThat(endpointUrlResult.getUri()).isEqualTo(uriResult.getUri());
        assertThat(endpointUrlResult.protocol()).isEqualTo(uriResult.protocol());
        assertThat(endpointUrlResult.host()).isEqualTo(uriResult.host());
        assertThat(endpointUrlResult.port()).isEqualTo(uriResult.port());
        assertThat(endpointUrlResult.encodedPath()).isEqualTo(uriResult.encodedPath());
    }

    @Test
    public void setUri_endpointUrlOverload_doubleSlash_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/a");
        URI resolvedUri = URI.create("https://override.example.com/a/b");

        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://override.example.com/a//c"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        SdkHttpRequest uriResult = AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri);
        SdkHttpRequest endpointUrlResult = AwsEndpointProviderUtils.setUri(request, clientEndpoint,
                                                                           EndpointUrl.parse(resolvedUri.toString()));

        assertThat(endpointUrlResult.getUri()).isEqualTo(uriResult.getUri());
        assertThat(endpointUrlResult.protocol()).isEqualTo(uriResult.protocol());
        assertThat(endpointUrlResult.host()).isEqualTo(uriResult.host());
        assertThat(endpointUrlResult.port()).isEqualTo(uriResult.port());
        assertThat(endpointUrlResult.encodedPath()).isEqualTo(uriResult.encodedPath());
    }

    @Test
    public void setUri_endpointUrlOverload_withTrailingSlashNoPath_combinesPathsCorrectly() {
        URI clientEndpoint = URI.create("https://override.example.com/");
        URI resolvedUri = URI.create("https://override.example.com/");

        SdkHttpRequest request = SdkHttpRequest.builder()
                                               .uri(URI.create("https://override.example.com//a"))
                                               .method(SdkHttpMethod.GET)
                                               .build();

        SdkHttpRequest uriResult = AwsEndpointProviderUtils.setUri(request, clientEndpoint, resolvedUri);
        SdkHttpRequest endpointUrlResult = AwsEndpointProviderUtils.setUri(request, clientEndpoint,
                                                                           EndpointUrl.parse(resolvedUri.toString()));

        assertThat(endpointUrlResult.getUri()).isEqualTo(uriResult.getUri());
        assertThat(endpointUrlResult.protocol()).isEqualTo(uriResult.protocol());
        assertThat(endpointUrlResult.host()).isEqualTo(uriResult.host());
        assertThat(endpointUrlResult.port()).isEqualTo(uriResult.port());
        assertThat(endpointUrlResult.encodedPath()).isEqualTo(uriResult.encodedPath());
    }

    // --- Property 9: addHostPrefix equivalence ---
    // Validates: Requirements 6.1, 6.2, 6.3

    @Test
    public void addHostPrefix_endpointUrl_prefixPresent_returnsPrefixPrepended() {
        Endpoint e = Endpoint.builder()
                             .endpointUrl(EndpointUrl.parse("https://foo.aws"))
                             .build();

        Endpoint result = AwsEndpointProviderUtils.addHostPrefix(e, "api.");

        assertThat(result.url()).isEqualTo(URI.create("https://api.foo.aws"));
        assertThat(result.endpointUrl().host()).isEqualTo("api.foo.aws");
        assertThat(result.endpointUrl().scheme()).isEqualTo("https");
    }

    @Test
    public void addHostPrefix_endpointUrl_preservesPortAndPath() {
        Endpoint e = Endpoint.builder()
                             .endpointUrl(EndpointUrl.parse("https://foo.aws:1234/a/b/c"))
                             .build();

        Endpoint result = AwsEndpointProviderUtils.addHostPrefix(e, "api.");

        assertThat(result.url()).isEqualTo(URI.create("https://api.foo.aws:1234/a/b/c"));
        assertThat(result.endpointUrl().host()).isEqualTo("api.foo.aws");
        assertThat(result.endpointUrl().port()).isEqualTo(1234);
        assertThat(result.endpointUrl().encodedPath()).isEqualTo("/a/b/c");
    }

    @Test
    public void addHostPrefix_endpointUrl_prefixIsNull_returnsUnModified() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://foo.aws");
        Endpoint e = Endpoint.builder()
                             .endpointUrl(endpointUrl)
                             .build();

        Endpoint result = AwsEndpointProviderUtils.addHostPrefix(e, null);

        assertThat(result.endpointUrl()).isEqualTo(endpointUrl);
        assertThat(result.url()).isEqualTo(URI.create("https://foo.aws"));
    }

    @Test
    public void addHostPrefix_endpointUrl_prefixIsEmpty_returnsUnModified() {
        EndpointUrl endpointUrl = EndpointUrl.parse("https://foo.aws");
        Endpoint e = Endpoint.builder()
                             .endpointUrl(endpointUrl)
                             .build();

        Endpoint result = AwsEndpointProviderUtils.addHostPrefix(e, "");

        assertThat(result.endpointUrl()).isEqualTo(endpointUrl);
        assertThat(result.url()).isEqualTo(URI.create("https://foo.aws"));
    }

    @Test
    public void addHostPrefix_endpointUrl_equivalentToUriPath() {
        // Verify that building an Endpoint via EndpointUrl.parse and then calling addHostPrefix
        // produces the same URL as building via URI and calling addHostPrefix
        String urlString = "https://foo.aws:8080/some/path";

        Endpoint fromUri = Endpoint.builder()
                                   .url(URI.create(urlString))
                                   .build();
        Endpoint fromEndpointUrl = Endpoint.builder()
                                           .endpointUrl(EndpointUrl.parse(urlString))
                                           .build();

        Endpoint resultFromUri = AwsEndpointProviderUtils.addHostPrefix(fromUri, "api.");
        Endpoint resultFromEndpointUrl = AwsEndpointProviderUtils.addHostPrefix(fromEndpointUrl, "api.");

        assertThat(resultFromEndpointUrl.url()).isEqualTo(resultFromUri.url());
    }


    private static ClientEndpointProvider endpointProvider(boolean isEndpointOverridden) {
        return ClientEndpointProvider.create(URI.create("https://foo.aws"), isEndpointOverridden);
    }
}
