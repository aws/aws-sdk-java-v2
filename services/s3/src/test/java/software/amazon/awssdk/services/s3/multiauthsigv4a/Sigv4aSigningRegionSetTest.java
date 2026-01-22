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

package software.amazon.awssdk.services.s3.multiauthsigv4a;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

@WireMockTest
@DisplayName("S3 SigV4a Signing Region Set Tests")
class Sigv4aSigningRegionSetTest {
    private static final String EXAMPLE_BUCKET = "Example-Bucket";
    private static final String EXAMPLE_RESPONSE_BODY = "Hello world";
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    public static final Function<InputStream, String> stringFromStream = inputStream ->
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

    private final MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
    private final MockAsyncHttpClient mockAsyncHttpClient = new MockAsyncHttpClient();

    @AfterEach
    void cleanUp() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
    }

    private static Stream<Arguments> regionSetConfigurations() {
        return Stream.of(
            Arguments.of("ClientBuilder Configuration", Region.EU_NORTH_1, "eu-north-1", "CLIENT_BUILDER"),
            Arguments.of("Environment Variable", Region.EU_NORTH_1, "eu-north-1", "ENVIRONMENT"),
            Arguments.of("Endpoint Override", Region.US_EAST_1, "us-*", "ENDPOINT_OVERRIDE"),
            Arguments.of("Client Region", Region.EU_CENTRAL_1, "eu-central-1", "CLIENT_REGION")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("regionSetConfigurations")
    @DisplayName("Async GetObject with different region set configurations")
    void asyncGetObjectWithDifferentRegionSetConfigurations(String testName, Region region,
                                                            String expectedRegionSet, String configType,
                                                            WireMockRuntimeInfo wm) {

        RegionSet regionSet = RegionSet.create(expectedRegionSet);
        setupMockResponses();

        S3AsyncClient s3Client = buildAsyncClientWithConfiguration(wm, region, regionSet, configType);

        s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key"),
                           AsyncResponseTransformer.toBytes()).join().asUtf8String();

        verifyHeaders(mockAsyncHttpClient.getLastRequest().headers(), expectedRegionSet);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("regionSetConfigurations")
    @DisplayName("Sync GetObject with different region set configurations")
    void syncGetObjectWithDifferentRegionSetConfigurations(String testName, Region region,
                                                           String expectedRegionSet, String configType,
                                                           WireMockRuntimeInfo wm) {
        RegionSet regionSet = RegionSet.create(expectedRegionSet);
        setupMockResponses();

        S3Client s3Client = buildSyncClientWithConfiguration(wm, region, regionSet, configType);

        s3Client.getObject(r -> r.bucket(EXAMPLE_BUCKET).key("key"),
                           ResponseTransformer.toBytes()).asUtf8String();

        verifyHeaders(mockHttpClient.getLastRequest().headers(), expectedRegionSet);
    }

    private void setupMockResponses() {
        stubFor(any(anyUrl()).willReturn(aResponse()
                                             .withStatus(200)
                                             .withBody(EXAMPLE_RESPONSE_BODY)));

        HttpExecuteResponse mockResponse = HttpExecuteResponse.builder()
                                                              .response(SdkHttpResponse.builder().statusCode(200).build())
                                                              .build();

        mockHttpClient.stubNextResponse(mockResponse);
        mockAsyncHttpClient.stubNextResponse(mockResponse);
    }

    private S3AsyncClient buildAsyncClientWithConfiguration(WireMockRuntimeInfo wm, Region region,
                                                            RegionSet regionSet, String configType) {
        S3AsyncClientBuilder builder = getAsyncClientBuilder(wm)
            .httpClient(mockAsyncHttpClient);

        switch (configType) {
            case "CLIENT_BUILDER":
                builder.sigv4aSigningRegionSet(regionSet);
                break;
            case "ENVIRONMENT":
                ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, regionSet.asString());
                break;
            case "CLIENT_REGION":
                builder.region(region);
                break;
        }

        builder.endpointProvider(createEndpointProvider(configType));
        return builder.build();
    }

    private S3Client buildSyncClientWithConfiguration(WireMockRuntimeInfo wm, Region region,
                                                      RegionSet regionSet, String configType) {
        S3ClientBuilder builder = getSyncClientBuilder(wm)
            .httpClient(mockHttpClient);

        switch (configType) {
            case "CLIENT_BUILDER":
                builder.sigv4aSigningRegionSet(regionSet);
                break;
            case "ENVIRONMENT":
                ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET, regionSet.asString());
                break;
            case "CLIENT_REGION":
                builder.region(region);
                break;
        }

        builder.endpointProvider(createEndpointProvider(configType));
        return builder.build();
    }

    private S3EndpointProvider createEndpointProvider(String configType) {
        return new S3EndpointProvider() {
            @Override
            public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams endpointParams) {
                return CompletableFuture.completedFuture(
                    configType.equals("CLIENT_REGION") ?
                    createClientRegionEndpoint() :
                    customEndPointForBucket(EXAMPLE_BUCKET)
                );
            }
        };
    }

    private void verifyHeaders(Map<String, List<String>> headers, String expectedRegionSet) {
        assertThat(headers.get("X-Amz-Region-Set").get(0)).isEqualTo(expectedRegionSet);
        assertThat(headers.get("Authorization").get(0)).contains("AWS4-ECDSA-P256-SHA256");
    }

    private S3ClientBuilder getSyncClientBuilder(WireMockRuntimeInfo wm) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                       .credentialsProvider(
                           StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    private S3AsyncClientBuilder getAsyncClientBuilder(WireMockRuntimeInfo wm) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                            .credentialsProvider(
                                StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    private static Endpoint customEndPointForBucket(String bucketName) {
        return Endpoint.builder()
                       .url(URI.create("https://" + bucketName + ".s3.us-east-1.amazonaws.com"))
                       .putAttribute(
                           AwsEndpointAttribute.AUTH_SCHEMES,
                           Arrays.asList(
                               SigV4aAuthScheme.builder()
                                               .disableDoubleEncoding(true)
                                               .signingName("s3")
                                               .signingRegionSet(Collections.singletonList("us-*"))
                                               .build(),
                               SigV4AuthScheme.builder()
                                              .disableDoubleEncoding(true)
                                              .signingName("s3")
                                              .signingRegion("us-east-1")
                                              .build()))
                       .build();
    }

    private static Endpoint createClientRegionEndpoint() {
        return Endpoint.builder()
                       .url(URI.create("https://" + EXAMPLE_BUCKET + ".s3.us-east-1.amazonaws.com"))
                       .putAttribute(
                           AwsEndpointAttribute.AUTH_SCHEMES,
                           Collections.singletonList(
                               SigV4aAuthScheme.builder()
                                               .disableDoubleEncoding(true)
                                               .signingName("s3")
                                               .build()))
                       .build();
    }
}
