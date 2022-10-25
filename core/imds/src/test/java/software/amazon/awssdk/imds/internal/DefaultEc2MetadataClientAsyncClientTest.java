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

package software.amazon.awssdk.imds.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.MetadataResponse;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEc2MetadataClientAsyncClientTest {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    private static final String EC2_METADATA_ROOT = "/latest/meta-data";
    private static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    @Test
    public void get_successOnFirstTry_shouldNotRetryAndCompleteSuccessfully() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + mockMetadataEndpoint.port());
        try (Ec2MetadataAsyncClient client =
                Ec2MetadataAsyncClient.builder()
                                      .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                      .tokenTtl(Duration.ofSeconds(1024))
                                      .build())
        {
            CompletableFuture<MetadataResponse> res = client.get(AMI_ID_RESOURCE);
            assertThat(res).isNotCompleted();
            MetadataResponse response = res.get();
            assertThat(res).isCompleted();
            assertThat(response.asString()).isEqualTo("{}");
            verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("1024")));
            verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        } catch (Exception e) {
            fail("Unexpected exception while executing test", e);
        }
    }

    @Test
    public void get_failsEverytime_shouldRetryThreeTimesAndCompleteExceptionally() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + mockMetadataEndpoint.port());
        try (Ec2MetadataAsyncClient client =
                 Ec2MetadataAsyncClient.builder()
                                       .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                       .tokenTtl(Duration.ofSeconds(1024))
                                       .build())
        {
            CompletableFuture<MetadataResponse> res = client.get(AMI_ID_RESOURCE);
            assertThat(res).isNotCompleted();
            assertThatThrownBy(res::get).isInstanceOf(ExecutionException.class);
            assertThat(res).isCompletedExceptionally();
            verify(exactly(4), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("1024")));
            verify(exactly(4), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        } catch (Exception e) {
            fail("Unexpected exception while executing test", e);
        }
    }

    @Test
    public void getToken_failsEverytime_shouldRetryThreeTimesAndCompleteExceptionallyAndNotCallService() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + mockMetadataEndpoint.port());
        try (Ec2MetadataAsyncClient client =
                 Ec2MetadataAsyncClient.builder()
                                       .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                       .tokenTtl(Duration.ofSeconds(1024))
                                       .build())
        {
            Logger log = LoggerFactory.getLogger(DefaultEc2MetadataClientAsyncClientTest.class);
            log.error("TEST LOGGER TEST");
            CompletableFuture<MetadataResponse> res = client.get(AMI_ID_RESOURCE);
            assertThat(res).isNotCompleted();
            assertThatThrownBy(res::get).isInstanceOf(ExecutionException.class);
            assertThat(res).isCompletedExceptionally();
            verify(exactly(4), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("1024")));
            verify(exactly(0), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        } catch (Exception e) {
            fail("Unexpected exception while executing test", e);
        }
    }

    @Test
    public void get_returnsStatus4XX_shouldCompleteExceptionallyAndNotRetry() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withStatus(400).withBody("error")));

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + mockMetadataEndpoint.port());
        try (Ec2MetadataAsyncClient client =
                 Ec2MetadataAsyncClient.builder()
                                       .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                       .tokenTtl(Duration.ofSeconds(1024))
                                       .build())
        {
            CompletableFuture<MetadataResponse> res = client.get(AMI_ID_RESOURCE);
            assertThat(res).isNotCompleted();
            assertThatThrownBy(res::get).isInstanceOf(ExecutionException.class);
            assertThat(res).isCompletedExceptionally();
            verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("1024")));
            verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        } catch (Exception e) {
            fail("Unexpected exception while executing test", e);
        }

    }

    @Test
    public void get_builderWithAlCustomProperty_shouldSucceed() {
        Ec2MetadataRetryPolicy retryPolicy =
            Ec2MetadataRetryPolicy.builder()
                                  .numRetries(5)
                                  .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(100))).build();
        Ec2MetadataAsyncClient.Builder builder =
            Ec2MetadataAsyncClient.builder()
                                  .endpoint(URI.create("custom/end-point"))
                                  .endpointMode(EndpointMode.IPV6)
                                  .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(10).build())
                                  .retryPolicy(retryPolicy)
                                  .tokenTtl(Duration.ofSeconds(1024))
                                  .scheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
        try (Ec2MetadataAsyncClient client = builder.build()) {

        } catch (Exception e) {
            fail("Unexpected exception while executing test", e);
        }
    }

    @Test
    public void get_multipleAsyncRequest_shouldCompleteSuccessfully() {
        int totalRequest = 10;
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(
            aResponse().withFixedDelay(1000).withBody("some-token")));
        for (int i = 0; i < totalRequest; i++) {
            ResponseDefinitionBuilder responseStub = aResponse().withFixedDelay(1000)
                                                                .withStatus(200)
                                                                .withBody("response::" + i);
            stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE + "/" + i)).willReturn(responseStub));
        }
        try (Ec2MetadataAsyncClient client =
                 Ec2MetadataAsyncClient.builder()
                                       .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                       .tokenTtl(Duration.ofSeconds(1024))
                                       .build())
        {
            List<CompletableFuture<MetadataResponse>> requests = Stream.iterate(0, x -> x + 1)
                                                                       .map(i -> client.get(AMI_ID_RESOURCE + "/" + i))
                                                                       .limit(totalRequest)
                                                                       .collect(Collectors.toList());
            CompletableFuture<List<MetadataResponse>> responses =
                CompletableFuture.allOf(requests.toArray(new CompletableFuture[0]))
                                 .thenApply(unusedVoid -> requests.stream()
                                                                  .map(CompletableFuture::join)
                                                                  .collect(Collectors.toList()));

            List<MetadataResponse> resolvedResponses = responses.join();
            for (int i = 0; i < totalRequest; i++) {
                MetadataResponse response = resolvedResponses.get(i);
                assertThat(response.asString()).isEqualTo("response::" + i);
            }
            verify(exactly(totalRequest), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("1024")));
            verify(exactly(totalRequest), getRequestedFor(urlPathMatching(AMI_ID_RESOURCE + "/" + "\\d"))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        } catch (Exception e) {
            fail("Unexpected exception while executing test", e);
        }
    }
}