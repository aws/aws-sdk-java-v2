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

package software.amazon.awssdk.services.s3;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RunWith(MockitoJUnitRunner.class)
public class S3SignerTest {

    public static final ChecksumSpecs CRC32_TRAILER = ChecksumSpecs.builder().algorithm(Algorithm.CRC32)
                                                                   .isRequestStreaming(true)
                                                                   .headerName("x-amz-checksum-crc32").build();

    public static final ChecksumSpecs SHA256_HEADER = ChecksumSpecs.builder().algorithm(Algorithm.SHA256)
                                                                   .isRequestStreaming(false)
                                                                   .headerName("x-amz-checksum-sha256").build();

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    private S3Client getS3Client(boolean chunkedEncoding, boolean payloadSigning, URI endpoint) {
        return S3Client.builder()
                       .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                         .putExecutionAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING, chunkedEncoding)
                                                                         .putExecutionAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, payloadSigning)
                                                                         .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                                            AwsS3V4Signer.create()).build())
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                       .region(Region.US_EAST_2).endpointOverride(endpoint)
                       .serviceConfiguration(c -> c.checksumValidationEnabled(false).pathStyleAccessEnabled(true))
                       .build();
    }

    @Test
    public void payloadSigningWithChecksum() {

        S3Client s3Client = getS3Client(true, true, URI.create(getEndpoint()));
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(PutObjectRequest.builder()
                                           .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                           .bucket("test").key("test").build(), RequestBody.fromBytes("abc".getBytes()));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("296")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-trailer", equalTo(CRC32_TRAILER.headerName())));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-decoded-content-length", equalTo("3")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER"
        )));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("aws-chunked")));
        verify(putRequestedFor(anyUrl()).withRequestBody(containing("x-amz-checksum-crc32:NSRBwg=="))
                                        .withRequestBody(containing("x-amz-trailer-signature:"))
                                        .withRequestBody(containing("0;")));
    }

    @Test
    public void payloadSigningWithChecksumWithContentEncodingSuppliedByUser() {

        S3Client s3Client = getS3Client(true, true, URI.create(getEndpoint()));
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(PutObjectRequest.builder()
                                           .checksumAlgorithm(ChecksumAlgorithm.CRC32).contentEncoding("deflate")
                                           .bucket("test").key("test").build(), RequestBody.fromBytes("abc".getBytes()));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("296")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-trailer", equalTo(CRC32_TRAILER.headerName())));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-decoded-content-length", equalTo("3")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-AWS4-HMAC-SHA256-PAYLOAD-TRAILER"
        )));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("aws-chunked")));
        verify(putRequestedFor(anyUrl()).withHeader("Content-Encoding", equalTo("deflate")));
        verify(putRequestedFor(anyUrl()).withRequestBody(containing("x-amz-checksum-crc32:NSRBwg=="))
                                        .withRequestBody(containing("x-amz-trailer-signature:"))
                                        .withRequestBody(containing("0;")));
    }

    @Test
    public void payloadSigningWithNoChecksum() {

        S3Client s3Client = getS3Client(true, true, URI.create(getEndpoint()));
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(PutObjectRequest.builder()
                                           .bucket("test").key("test").build(), RequestBody.fromBytes("abc".getBytes()));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("175")));
        verify(putRequestedFor(anyUrl()).withoutHeader("x-amz-trailer"));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-decoded-content-length", equalTo("3")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", equalTo("STREAMING-AWS4-HMAC-SHA256-PAYLOAD"
        )));
        verify(putRequestedFor(anyUrl()).withoutHeader("Content-Encoding"));
        verify(putRequestedFor(anyUrl()).withRequestBody(notMatching("x-amz-checksum-crc32:NSRBwg=="))
                                        .withRequestBody(notMatching("x-amz-trailer-signature:"))
                                        .withRequestBody(notMatching("0;")));
    }

    @Test
    public void headerBasedSignedPayload() {
        S3Client s3Client = getS3Client(false, false, URI.create(getEndpoint()));
        stubFor(any(urlMatching(".*"))
                    .willReturn(response()));
        s3Client.putObject(PutObjectRequest.builder()
                               .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                                           .bucket("test").key("test").build(), RequestBody.fromBytes("abc".getBytes()));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_TYPE, equalTo(Mimetype.MIMETYPE_OCTET_STREAM)));
        verify(putRequestedFor(anyUrl()).withHeader(CONTENT_LENGTH, equalTo("3")));
        verify(putRequestedFor(anyUrl()).withHeader(SHA256_HEADER.headerName(), equalTo("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD"
                                                                                        + "/YfIAFa0=")));
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", notMatching("STREAMING-AWS4-HMAC-SHA256-PAYLOAD"
                                                                                        + "-TRAILER")));
        // This keeps changing based on time so matching if a valid string exist as signature.
        // TODO : mock the clock and make the signature static for given time.
        verify(putRequestedFor(anyUrl()).withHeader("x-amz-content-sha256", matching("\\w+")));
        verify(putRequestedFor(anyUrl()).withoutHeader("x-amz-trailer"));
        verify(putRequestedFor(anyUrl()).withoutHeader("Content-Encoding"));
    }

    private ResponseDefinitionBuilder response() {
        return aResponse().withStatus(200).withHeader(CONTENT_LENGTH, "0").withBody("");
    }
}