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

package software.amazon.awssdk.protocol.tests.crc32;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.Crc32MismatchException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocoljsonrpc.ProtocolJsonRpcAsyncClient;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesResponse;
import software.amazon.awssdk.services.protocoljsonrpccustomized.ProtocolJsonRpcCustomizedAsyncClient;
import software.amazon.awssdk.services.protocoljsonrpccustomized.model.SimpleRequest;
import software.amazon.awssdk.services.protocoljsonrpccustomized.model.SimpleResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class AwsJsonAsyncCrc32ChecksumTests {
    @Rule
    public WireMockRule mockServer = new WireMockRule(WireMockConfiguration.wireMockConfig()
                                                                           .port(0)
                                                                           .fileSource(new SingleRootFileSource
                                                                                           ("src/test/resources")));

    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    private static final String JSON_BODY_GZIP = "compressed_json_body.gz";
    private static final String JSON_BODY_Crc32_CHECKSUM = "3049587505";
    private static final String JSON_BODY_GZIP_Crc32_CHECKSUM = "3023995622";

    private static final String JSON_BODY_EXTRA_DATA_GZIP = "compressed_json_body_with_extra_data.gz";
    private static final String JSON_BODY_EXTRA_DATA_GZIP_Crc32_CHECKSUM = "1561543715";

    private static final StaticCredentialsProvider FAKE_CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"));
    private ProtocolJsonRpcCustomizedAsyncClient customizedJsonRpcAsync;
    private ProtocolJsonRpcAsyncClient jsonRpcAsync;

    @Before
    public void setup() {
        jsonRpcAsync = ProtocolJsonRpcAsyncClient.builder()
                                                 .credentialsProvider(FAKE_CREDENTIALS_PROVIDER)
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                                 .build();

        customizedJsonRpcAsync = ProtocolJsonRpcCustomizedAsyncClient.builder()
                                                                     .credentialsProvider(FAKE_CREDENTIALS_PROVIDER)
                                                                     .region(Region.US_EAST_1)
                                                                     .endpointOverride(URI.create("http://localhost:" +
                                                                                                  mockServer.port()))
                                                                     .build();
    }

    @Test
    public void clientCalculatesCrc32FromCompressedData_WhenCrc32IsValid() throws ExecutionException, InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Encoding", "gzip")
                                                     .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                                                     .withBodyFile(JSON_BODY_GZIP)));

        SimpleResponse result = customizedJsonRpcAsync.simple(SimpleRequest.builder().build()).get();
        Assert.assertEquals("foo", result.stringMember());
    }

    /**
     * See https://github.com/aws/aws-sdk-java/issues/1018. With GZIP there is apparently a chance there can be some extra
     * stuff/padding beyond the JSON document. Jackson's JsonParser won't necessarily read this if it's able to close the JSON
     * object. After unmarshalling the response, the SDK should consume all the remaining bytes from the stream to ensure the
     * Crc32 calculated is accurate.
     */
    @Test
    public void clientCalculatesCrc32FromCompressedData_ExtraData_WhenCrc32IsValid() throws ExecutionException,
                                                                                            InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Encoding", "gzip")
                                                     .withHeader("x-amz-crc32", JSON_BODY_EXTRA_DATA_GZIP_Crc32_CHECKSUM)
                                                     .withBodyFile(JSON_BODY_EXTRA_DATA_GZIP)));

        SimpleResponse result = customizedJsonRpcAsync.simple(SimpleRequest.builder().build()).get();
        Assert.assertEquals("foo", result.stringMember());
    }

    @Test
    public void clientCalculatesCrc32FromCompressedData_WhenCrc32IsInvalid_ThrowsException() throws ExecutionException,
                                                                                                    InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Encoding", "gzip")
                                                     .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                                                     .withBodyFile(JSON_BODY_GZIP)));

        assertThatThrownBy(() -> customizedJsonRpcAsync.simple(SdkBuilder::build).get())
            .hasRootCauseInstanceOf(Crc32MismatchException.class);
    }

    @Test
    public void clientCalculatesCrc32FromDecompressedData_WhenCrc32IsValid() throws ExecutionException, InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Encoding", "gzip")
                                                     .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                                                     .withBodyFile(JSON_BODY_GZIP)));

        AllTypesResponse result =
            jsonRpcAsync.allTypes(AllTypesRequest.builder().build()).get();
        Assert.assertEquals("foo", result.stringMember());
    }

    @Test
    public void clientCalculatesCrc32FromDecompressedData_WhenCrc32IsInvalid_ThrowsException() throws ExecutionException,
                                                                                                      InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("Content-Encoding", "gzip")
                                                     .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                                                     .withBodyFile(JSON_BODY_GZIP)));

        assertThatThrownBy(() -> jsonRpcAsync.allTypes(AllTypesRequest.builder().build()).get())
            .hasRootCauseInstanceOf(Crc32MismatchException.class);
    }

    @Test
    public void useGzipFalse_WhenCrc32IsValid() throws ExecutionException, InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                                                     .withBody(JSON_BODY)));

        AllTypesResponse result =
            jsonRpcAsync.allTypes(AllTypesRequest.builder().build()).get();
        Assert.assertEquals("foo", result.stringMember());
    }

    @Test
    public void useGzipFalse_WhenCrc32IsInvalid_ThrowException() throws ExecutionException, InterruptedException {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                     .withStatus(200)
                                                     .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                                                     .withBody(JSON_BODY)));

        assertThatThrownBy(() -> jsonRpcAsync.allTypes(AllTypesRequest.builder().build()).get())
            .hasRootCauseInstanceOf(Crc32MismatchException.class);
    }
}
