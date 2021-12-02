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

package software.amazon.awssdk.services.s3.internal.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;

public class S3ObjectLambdaOperationEndpointResolverTest {
    private S3ObjectLambdaOperationEndpointResolver endpointResolver;

    @Before
    public void setUp() {
        endpointResolver = S3ObjectLambdaOperationEndpointResolver.create();
    }

    @Test
    public void writeGetObjectResponse_shouldConvertEndpoint() {
        String requestRoute = "route";
        String region = "us-west-2";
        String expectedHost = "route.s3-object-lambda.us-west-2.amazonaws.com";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, null, URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, null, URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    public void writeGetObjectResponse_preservesPathAndQueryParams() {
        Map<String, List<String>> expectedQueryParams = new HashMap<>();
        expectedQueryParams.put("foo", Collections.singletonList("bar"));
        String path = "/path";

        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                .originalRequest(WriteGetObjectResponseRequest.builder()
                        .requestRoute("route")
                        .build())
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().build())
                .request(SdkHttpRequest.builder()
                        .method(SdkHttpMethod.GET)
                        .protocol("https")
                        .host("route.s3.us-east-1.amazonaws.com")
                        .encodedPath(path)
                        .rawQueryParameters(expectedQueryParams)
                        .build())
                .build();

        ConfiguredS3SdkHttpRequest configuredS3SdkHttpRequest = endpointResolver.applyEndpointConfiguration(context);
        SdkHttpRequest httpRequest = configuredS3SdkHttpRequest.sdkHttpRequest();

        assertThat(httpRequest.encodedPath()).isEqualTo(path);
        assertThat(httpRequest.rawQueryParameters()).isEqualTo(expectedQueryParams);
    }

    @Test
    public void writeGetObjectResponse_nonStandardPartitionRegion_shouldConvertEndpointWithCorrectDnsSuffix() {
        String requestRoute = "route";
        String region = "cn-north-1";
        String expectedHost = "route.s3-object-lambda.cn-north-1.amazonaws.com.cn";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, null, URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, null, URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    public void writeGetObjectResponse_endpointOverridden_shouldConvertEndpoint() {
        String requestRoute = "route";
        String region = "us-west-2";
        String endpointOverride = "my-endpoint.com";
        String expectedHost = "route.my-endpoint.com";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, URI.create("https://" + endpointOverride), URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, URI.create("http://" + endpointOverride), URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    @Ignore // TODO: Taken from the SEP but this test case is suspect. The SEP
    // expects s3-external-1 to be translated to the us-east-1 region for this
    // operation, which is weird. Currently, we do not do this. 's3-external-1'
    // doesn't exist in endpoints.json, so we use heuristics to get the
    // endpoint s3.s3-external-1.amazonaws.com.
    public void writeGetObjectResponse_regionIsS3External_shouldConvertEndpoint() {
        String requestRoute = "route";
        String region = "s3-external-1";
        String expectedHost = "route.s3-object-lambda.us-east-1.amazonaws.com";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, null, URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, null, URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    @Ignore // TODO: Taken from the SEP but this test case is suspect. The SEP
    // expects aws-global to be translated to the us-east-1 region for this
    // operation, which is weird. We do not do this; we lookup the endpoint
    // from the region metadata which maps aws-global => s3.amazonaws.com, so
    // the resulting endpoint is s3-object-lambda.amazonaws.com
    public void writeGetObjectResponse_regionIsAwsGlobal_shouldConvertEndpoint() {
        String requestRoute = "route";
        String region = "aws-global";
        String expectedHost = "route.s3-object-lambda.us-east-1.amazonaws.com";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, null, URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, null, URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    @Ignore // SDK doesn't resolve fips endpoints correctly
    public void writeGetObjectResponse_regionIsFipsInPrefix_shouldConvertEndpoint() {
        String requestRoute = "route";
        String region = "fips-us-gov-east-1";
        String expectedHost = "route.s3-object-lambda-fips.us-gov-east-1.amazonaws.com";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, null, URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, null, URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    @Ignore // SDK doesn't resolve fips endpoints correctly
    public void writeGetObjectResponse_regionIsFipsInSuffix_shouldConvertEndpoint() {
        String requestRoute = "route";
        String region = "us-gov-east-1-fips";
        String expectedHost = "route.s3-object-lambda-fips.us-gov-east-1.amazonaws.com";
        WriteGetObjectResponseRequest request = WriteGetObjectResponseRequest.builder()
                .requestRoute(requestRoute)
                .build();

        verifyObjectLambdaEndpoint("https", request, region, null, URI.create("https://" + expectedHost), S3Configuration.builder());
        verifyObjectLambdaEndpoint("http", request, region, null, URI.create("http://" + expectedHost), S3Configuration.builder());
    }

    @Test
    public void nonObjectLambdaRequest_throws() {
        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                .originalRequest(PutObjectRequest.builder().build())
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().build())
                .request(InterceptorTestUtils.sdkHttpRequest(URI.create("https://bucket.my-custom-domain.com")))
                .build();

        assertThatThrownBy(() -> endpointResolver.applyEndpointConfiguration(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an S3 Object Lambda operation");
    }

    @Test
    public void objectLambdaOperation_dualStackEnabled_throws() {
        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                .originalRequest(WriteGetObjectResponseRequest.builder().build())
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().dualstackEnabled(true).build())
                .request(InterceptorTestUtils.sdkHttpRequest(URI.create("https://bucket.my-custom-domain.com")))
                .build();

        assertThatThrownBy(() -> endpointResolver.applyEndpointConfiguration(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Object Lambda does not support dualstack");
    }

    @Test
    public void objectLambdaOperation_accelerateEnabled_throws() {
        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                .originalRequest(WriteGetObjectResponseRequest.builder().build())
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().accelerateModeEnabled(true).build())
                .request(InterceptorTestUtils.sdkHttpRequest(URI.create("https://bucket.my-custom-domain.com")))
                .build();

        assertThatThrownBy(() -> endpointResolver.applyEndpointConfiguration(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Object Lambda does not support accelerate");
    }

    private void verifyObjectLambdaEndpoint(String protocol, S3Request request, String region, URI endpointOverride,
                                            URI expectedEndpoint, S3Configuration.Builder serviceConfigurationBuilder) {
        URI defaultRequestEndpoint = ServiceMetadata.of("s3").endpointFor(Region.of(region));

        String prefix = "";
        if (request instanceof WriteGetObjectResponseRequest) {
            prefix = ((WriteGetObjectResponseRequest) request).requestRoute() + ".";
        }

        defaultRequestEndpoint = URI.create(protocol + "://" + prefix + defaultRequestEndpoint.toString());

        SdkHttpRequest httpRequest;
        if (endpointOverride != null) {
            endpointOverride = URI.create(protocol + "://" + prefix + endpointOverride.getHost());
            httpRequest = InterceptorTestUtils.sdkHttpRequest(endpointOverride);
        } else {
            httpRequest = InterceptorTestUtils.sdkHttpRequest(defaultRequestEndpoint);
        }

        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                .endpointOverride(endpointOverride)
                .originalRequest(request)
                .region(Region.of(region))
                .serviceConfiguration(serviceConfigurationBuilder.build())
                .request(httpRequest)
                .build();

        ConfiguredS3SdkHttpRequest configuredS3SdkHttpRequest = endpointResolver.applyEndpointConfiguration(context);
        assertThat(configuredS3SdkHttpRequest.signingServiceModification().get()).isEqualTo("s3-object-lambda");
        assertThat(configuredS3SdkHttpRequest.signingRegionModification()).isEmpty();
        assertThat(configuredS3SdkHttpRequest.sdkHttpRequest().getUri()).isEqualTo(expectedEndpoint);
    }
}
