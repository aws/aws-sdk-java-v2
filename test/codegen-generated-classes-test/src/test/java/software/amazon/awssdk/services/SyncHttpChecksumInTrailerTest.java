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

package software.amazon.awssdk.services;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;

public class SyncHttpChecksumInTrailerTest {
    private static final String CRLF = "\r\n";
    private static final String SCENARIO = "scenario";
    private static final String PATH = "/";
    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);
    private ProtocolRestJsonClient client;

    @Before
    public void setupClient() {
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();

    }

    @Test
    public void sync_streaming_NoSigner_appends_trailer_checksum() {
        stubResponseWithHeaders();

        client.putOperationWithChecksum(r -> r.checksumAlgorithm(ChecksumAlgorithm.CRC32), RequestBody.fromString("Hello world")
            , ResponseTransformer.toBytes());
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("52")));
        verify(putRequestedFor(anyUrl()).withHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, equalTo("x-amz-checksum-crc32")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-UNSIGNED-PAYLOAD-TRAILER")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-decoded-content-length", equalTo("11")));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("aws-chunked")));
        //b is hex value of 11.
        verify(putRequestedFor(anyUrl()).withRequestBody(
            containing(
                "b" + CRLF + "Hello world" + CRLF
                + "0" + CRLF
                + "x-amz-checksum-crc32:i9aeUg==" + CRLF + CRLF)));
    }

    @Test
    public void sync_streaming_NoSigner_appends_trailer_checksum_withContentEncodingSetByUser() {
        stubResponseWithHeaders();


        client.putOperationWithChecksum(r ->
            r.checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                            .contentEncoding("deflate"),
                                        RequestBody.fromString("Hello world"),
                                        ResponseTransformer.toBytes());
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("aws-chunked")));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("deflate")));
        //b is hex value of 11.
        verify(putRequestedFor(anyUrl()).withRequestBody(
            containing(
                "b" + CRLF + "Hello world" + CRLF
                + "0" + CRLF
                + "x-amz-checksum-crc32:i9aeUg==" + CRLF + CRLF)));
    }

    @Test
    public void sync_streaming_specifiedLengthIsLess_NoSigner_appends_trailer_checksum() {
        stubResponseWithHeaders();

        ContentStreamProvider provider = () -> new ByteArrayInputStream("Hello world".getBytes(StandardCharsets.UTF_8));
        // length of 5 truncates to "Hello"
        RequestBody requestBody = RequestBody.fromContentProvider(provider, 5, "text/plain");
        client.putOperationWithChecksum(r -> r.checksumAlgorithm(ChecksumAlgorithm.CRC32),
                                        requestBody,
                                        ResponseTransformer.toBytes());
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("46")));
        verify(putRequestedFor(anyUrl()).withHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, equalTo("x-amz-checksum-crc32")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-UNSIGNED-PAYLOAD-TRAILER")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-decoded-content-length", equalTo("5")));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("aws-chunked")));
        verify(putRequestedFor(anyUrl()).withRequestBody(
            containing(
                "5" + CRLF + "Hello" + CRLF
                + "0" + CRLF
                // 99GJgg== is the base64 encoded CRC32 of "Hello"
                + "x-amz-checksum-crc32:99GJgg==" + CRLF + CRLF)));
    }

    @Test
    public void syncStreaming_withRetry_NoSigner_shouldContainChecksum_fromInterceptors() {
        stubForFailureThenSuccess(500, "500");
        final String expectedRequestBody =
            "3" + CRLF + "abc" + CRLF
            + "0" + CRLF
            + "x-amz-checksum-sha256:ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=" + CRLF + CRLF;

        client.putOperationWithChecksum(r -> r.checksumAlgorithm(ChecksumAlgorithm.SHA256), RequestBody.fromString("abc"),
                                        ResponseTransformer.toBytes());
        List<LoggedRequest> requests = getRecordedRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0).getBody()).contains(expectedRequestBody.getBytes());
        assertThat(requests.get(1).getBody()).contains(expectedRequestBody.getBytes());
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo("text/plain; charset=UTF-8")));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("81")));
        verify(putRequestedFor(anyUrl()).withHeader(HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE, equalTo("x-amz-checksum-sha256")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-UNSIGNED-PAYLOAD-TRAILER")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-decoded-content-length", equalTo("3")));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("aws-chunked")));
        verify(putRequestedFor(anyUrl()).withRequestBody(
            containing(
                expectedRequestBody)));
    }

    private void stubResponseWithHeaders() {
        stubFor(put(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-foo-id", "foo")
                                           .withHeader("x-bar-id", "bar")
                                           .withHeader("x-foobar-id", "foobar")
                                           .withBody("{}")));
    }

    private void stubForFailureThenSuccess(int statusCode, String errorCode) {
        WireMock.reset();
        stubFor(put(urlEqualTo(PATH))
                    .inScenario(SCENARIO)
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("1")
                    .willReturn(aResponse()
                                    .withStatus(statusCode)
                                    .withHeader("x-amzn-ErrorType", errorCode)
                                    .withBody("{}")));

        stubFor(put(urlEqualTo(PATH))
                    .inScenario(SCENARIO)
                    .whenScenarioStateIs("1")
                    .willSetStateTo("2")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(JSON_BODY)));
    }

    private List<LoggedRequest> getRecordedRequests() {
        return findAll(putRequestedFor(urlEqualTo(PATH)));
    }

}
