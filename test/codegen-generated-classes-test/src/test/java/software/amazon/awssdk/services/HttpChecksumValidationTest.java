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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.protocolrestjson.model.ChecksumMode;
import software.amazon.awssdk.services.protocolrestjson.model.GetOperationWithChecksumResponse;
import software.amazon.awssdk.services.protocolrestjson.model.OperationWithChecksumNonStreamingResponse;
import software.amazon.awssdk.utils.ImmutableMap;

@WireMockTest
public class HttpChecksumValidationTest {

    private static final Map<String, String> ALGORITHM_TO_VALUE = ImmutableMap.<String, String>builder()
        .put("crc32", "i9aeUg==")
        .put("crc32c", "crUfeA==")
        .put("sha1", "e1AsOh9IyGCa4hLN+2Od7jlnP14=")
        .put("sha256", "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=")
        .put("crc64nvme", "OOJZ0D8xKts=")
        .build();

    private ProtocolRestJsonClient client;
    private ProtocolRestJsonAsyncClient asyncClient;

    public static Stream<Map.Entry<String, String>> checksumEntry() {
        return ALGORITHM_TO_VALUE.entrySet().stream();
    }

    @BeforeEach
    public void setupClient(WireMockRuntimeInfo wm) {
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(AnonymousCredentialsProvider.create())
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                       .overrideConfiguration(
                                           // TODO(sra-identity-and-auth): we should remove these
                                           //  overrides once we set up codegen to set chunk-encoding to true
                                           //  for requests that are streaming and checksum-enabled
                                           o -> o.addExecutionInterceptor(new CaptureChecksumValidationInterceptor())
                                                 .putExecutionAttribute(
                                                     ENABLE_CHUNKED_ENCODING, true
                                                 ))
                                       .build();

        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                 .region(Region.US_EAST_1)
                                                 .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureChecksumValidationInterceptor()))
                                                 .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                                 .build();
    }

    @AfterEach
    public void clear() {
        CaptureChecksumValidationInterceptor.reset();
    }

    @ParameterizedTest
    @MethodSource("checksumEntry")
    public void syncClientStreamingResponse_shouldValidate(Map.Entry<String, String> checksumToValue) {
        stubWithSingleChecksum("Hello world", checksumToValue.getValue(), checksumToValue.getKey());
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(r -> r.checksumMode(ChecksumMode.ENABLED), ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.fromValue(checksumToValue.getKey()));
    }

    @Test
    public void syncClientValidateStreamingResponse_multipleChecksumInResponse_shouldChooseBasedOnPriority() {
        stubWithMultipleChecksums("Hello world", ALGORITHM_TO_VALUE);
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(r -> r.checksumMode(ChecksumMode.ENABLED), ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32C);
    }

    @Test
    public void syncClientValidateStreamingResponseZeroByte() {
        String expectedChecksum = "AAAAAA==";
        stubWithSingleChecksum("", expectedChecksum, "crc32");
        client.putOperationWithChecksum(r -> r
                                            .checksumAlgorithm(ChecksumAlgorithm.CRC32),
                                        RequestBody.fromString(""));
        verify(putRequestedFor(urlEqualTo("/")).withHeader("x-amz-trailer", equalTo("x-amz-checksum-crc32")));
        verify(putRequestedFor(urlEqualTo("/")).withRequestBody(containing("x-amz-checksum-crc32:" + expectedChecksum)));
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(r -> r.checksumMode(ChecksumMode.ENABLED), ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEmpty();
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void syncClientValidateNonStreamingResponse() {
        String expectedChecksum = "lzlLIA==";
        stubWithSingleChecksum("{\"stringMember\":\"Hello world\"}", expectedChecksum, "crc32");
        client.operationWithChecksumNonStreaming(
            r -> r.stringMember("Hello world").checksumAlgorithm(ChecksumAlgorithm.CRC32)
                  // TODO(sra-identity-and-auth): we should remove these
                  //  overrides once we set up codegen to set chunk-encoding to true
                  //  for requests that are streaming and checksum-enabled
                  .overrideConfiguration(c -> c.putExecutionAttribute(ENABLE_CHUNKED_ENCODING, false))
        );
        verify(postRequestedFor(urlEqualTo("/")).withHeader("x-amz-checksum-crc32", equalTo(expectedChecksum)));
        OperationWithChecksumNonStreamingResponse operationWithChecksumNonStreamingResponse =
            client.operationWithChecksumNonStreaming(o -> o.checksumMode(ChecksumMode.ENABLED));
        assertThat(operationWithChecksumNonStreamingResponse.stringMember()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void syncClientValidateNonStreamingResponseZeroByte() {
        String expectedChecksum = "o6a/Qw==";
        stubWithSingleChecksum("{}", expectedChecksum, "crc32");
        client.operationWithChecksumNonStreaming(
            r -> r.checksumAlgorithm(ChecksumAlgorithm.CRC32)
                  // TODO(sra-identity-and-auth): we should remove these
                  //  overrides once we set up codegen to set chunk-encoding to true
                  //  for requests that are streaming and checksum-enabled
                  .overrideConfiguration(c -> c.putExecutionAttribute(ENABLE_CHUNKED_ENCODING, false))
        );
        verify(postRequestedFor(urlEqualTo("/")).withHeader("x-amz-checksum-crc32", equalTo(expectedChecksum)));
        OperationWithChecksumNonStreamingResponse operationWithChecksumNonStreamingResponse =
            client.operationWithChecksumNonStreaming(o -> o.checksumMode(ChecksumMode.ENABLED));
        assertThat(operationWithChecksumNonStreamingResponse.stringMember()).isNull();
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void syncClientValidateStreamingResponseForceSkip() {
        String expectedChecksum = "i9aeUg==";
        stubWithSingleChecksum("Hello world", expectedChecksum, "crc32");
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED).overrideConfiguration(
                    o -> o.putExecutionAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                                 ChecksumValidation.FORCE_SKIP)),
                ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.FORCE_SKIP);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @Test
    public void syncClientValidateStreamingResponse_noAlgorithmInResponse_shouldSkipValidation() {
        stubWithNoChecksum();
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED),
                ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"foobar1", "foobar2"})
    public void syncClientValidateStreamingResponse_noSupportedAlgorithmFoundInClient_shouldSkipValidation(String unsupportedAlgorithm) {
        String expectedChecksum = "someNewAlgorithmCalculatedValue";
        stubWithSingleChecksum("Hello world", expectedChecksum, unsupportedAlgorithm);
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED),
                ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @Test
    public void syncClientValidateStreamingResponseWithValidationFailed() {
        String expectedChecksum = "i9aeUg=";
        stubWithSingleChecksum("Hello world", expectedChecksum, "crc32");
        assertThatExceptionOfType(SdkClientException.class)
            .isThrownBy(() -> client.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED),
                ResponseTransformer.toBytes()))
            .withMessage("Unable to unmarshall response (Data read has a different checksum than expected. Was i9aeUg==, "
                         + "but expected i9aeUg=). Response Code: 200, Response Text: OK");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void syncClientSkipsValidationWhenFeatureDisabled() {
        String expectedChecksum = "i9aeUg==";
        stubWithSingleChecksum("Hello world", expectedChecksum, "crc32");
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            client.getOperationWithChecksum(r -> r.stringMember("foo"), ResponseTransformer.toBytes());
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isNull();
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @ParameterizedTest
    @MethodSource("checksumEntry")
    public void asyncClientStreamingResponse_shouldValidate(Map.Entry<String, String> checksumToValue) {
        stubWithSingleChecksum("Hello world", checksumToValue.getValue(), checksumToValue.getKey());
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(r -> r.checksumMode(ChecksumMode.ENABLED), AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.fromValue(checksumToValue.getKey()));
    }

    @Test
    public void asyncClientValidateStreamingResponse_multipleChecksumInResponse_shouldChooseBasedOnPriority() {
        stubWithMultipleChecksums("Hello world", ALGORITHM_TO_VALUE);
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(r -> r.checksumMode(ChecksumMode.ENABLED), AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32C);
    }

    @Test
    public void asyncClientValidateStreamingResponseZeroByte() {
        String expectedChecksum = "AAAAAA==";
        stubWithSingleChecksum("", expectedChecksum, "crc32");
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(r -> r.checksumMode(ChecksumMode.ENABLED), AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEmpty();
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void asyncClientValidateNonStreamingResponse() {
        String expectedChecksum = "lzlLIA==";
        stubWithSingleChecksum("{\"stringMember\":\"Hello world\"}", expectedChecksum, "crc32");
        OperationWithChecksumNonStreamingResponse response =
            asyncClient.operationWithChecksumNonStreaming(
                o -> o.checksumMode(ChecksumMode.ENABLED)
                      // TODO(sra-identity-and-auth): we should remove these
                      //  overrides once we set up codegen to set chunk-encoding to true
                      //  for requests that are streaming and checksum-enabled
                      .overrideConfiguration(c -> c.putExecutionAttribute(ENABLE_CHUNKED_ENCODING, false))).join();
        assertThat(response.stringMember()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void asyncClientValidateNonStreamingResponseZeroByte() {
        String expectedChecksum = "o6a/Qw==";
        stubWithSingleChecksum("{}", expectedChecksum, "crc32");
        OperationWithChecksumNonStreamingResponse operationWithChecksumNonStreamingResponse =
            asyncClient.operationWithChecksumNonStreaming(
                o -> o.checksumMode(ChecksumMode.ENABLED)
                      // TODO(sra-identity-and-auth): we should remove these
                      //  overrides once we set up codegen to set chunk-encoding to true
                      //  for requests that are streaming and checksum-enabled
                      .overrideConfiguration(c -> c.putExecutionAttribute(ENABLE_CHUNKED_ENCODING, false))).join();
        assertThat(operationWithChecksumNonStreamingResponse.stringMember()).isNull();
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void asyncClientValidateStreamingResponseForceSkip() {
        String expectedChecksum = "i9aeUg==";
        stubWithSingleChecksum("Hello world", expectedChecksum, "crc32");
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED).overrideConfiguration(
                    o -> o.putExecutionAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                                 ChecksumValidation.FORCE_SKIP)),
                AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.FORCE_SKIP);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @Test
    public void asyncClientValidateStreamingResponseNoAlgorithmInResponse() {
        stubWithNoChecksum();
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED),
                AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"foobar1", "foobar2"})
    public void asyncClientValidateStreamingResponseNoSupportedAlgorithmFoundInClient_shouldNotFail(String unsupportedAlgorithm) {
        String expectedChecksum = "someNewAlgorithmCalculatedValue";
        stubWithSingleChecksum("Hello world", expectedChecksum, unsupportedAlgorithm);
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED),
                AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    @Test
    public void asyncClientValidateStreamingResponseWithValidationFailed() {
        String expectedChecksum = "i9aeUg=";
        stubWithSingleChecksum("Hello world", expectedChecksum, "crc32");
        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(() -> asyncClient.getOperationWithChecksum(
                r -> r.checksumMode(ChecksumMode.ENABLED),
                AsyncResponseTransformer.toBytes()).join())

            .withMessage("software.amazon.awssdk.core.exception.SdkClientException: Data read has a different checksum"
                         + " than expected. Was i9aeUg==, but expected i9aeUg=");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isEqualTo(DefaultChecksumAlgorithm.CRC32);
    }

    @Test
    public void asyncClientSkipsValidationWhenFeatureDisabled() {
        String expectedChecksum = "i9aeUg==";
        stubWithSingleChecksum("Hello world", expectedChecksum, "crc32");
        ResponseBytes<GetOperationWithChecksumResponse> responseBytes =
            asyncClient.getOperationWithChecksum(r -> r.stringMember("foo"), AsyncResponseTransformer.toBytes()).join();
        assertThat(responseBytes.asUtf8String()).isEqualTo("Hello world");
        assertThat(CaptureChecksumValidationInterceptor.checksumValidation).isNull();
        assertThat(CaptureChecksumValidationInterceptor.expectedAlgorithm).isNull();
    }

    private void stubWithSingleChecksum(String body, String checksumValue, String algorithmName) {
        ResponseDefinitionBuilder responseBuilder = aResponse().withStatus(200)
                                                               .withHeader("x-amz-checksum-" + algorithmName, checksumValue)
                                                               .withHeader("content-length",
                                                                           String.valueOf(body.length()))
                                                               .withBody(body);
        stubFor(get(anyUrl()).willReturn(responseBuilder));
        stubFor(put(anyUrl()).willReturn(responseBuilder));
        stubFor(post(anyUrl()).willReturn(responseBuilder));
    }

    private void stubWithMultipleChecksums(String body, Map<String, String> checksums) {
        ResponseDefinitionBuilder responseBuilder = aResponse().withStatus(200)
                                                               .withHeader("content-length",
                                                                           String.valueOf(body.length()))
                                                               .withBody(body);


        checksums.entrySet().forEach(checksum -> responseBuilder.withHeader("x-amz-checksum-" + checksum.getKey(),
                                                                 checksum.getValue()));
        stubFor(get(anyUrl()).willReturn(responseBuilder));
        stubFor(put(anyUrl()).willReturn(responseBuilder));
        stubFor(post(anyUrl()).willReturn(responseBuilder));
    }

    private void stubWithNoChecksum() {
        ResponseDefinitionBuilder responseBuilder = aResponse().withStatus(200)
                                                               .withHeader("content-length",
                                                                           String.valueOf("Hello world".length()))
                                                               .withBody("Hello world");
        stubFor(get(anyUrl()).willReturn(responseBuilder));
        stubFor(put(anyUrl()).willReturn(responseBuilder));
        stubFor(post(anyUrl()).willReturn(responseBuilder));
    }

    private static class CaptureChecksumValidationInterceptor implements ExecutionInterceptor {
        private static software.amazon.awssdk.checksums.spi.ChecksumAlgorithm expectedAlgorithm;
        private static ChecksumValidation checksumValidation;

        public static void reset() {
            expectedAlgorithm = null;
            checksumValidation = null;

        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            expectedAlgorithm =
                executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM_V2).orElse(null);
            checksumValidation =
                executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION).orElse(null);
        }

        @Override
        public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
            expectedAlgorithm =
                executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM_V2).orElse(null);
            checksumValidation =
                executionAttributes.getOptionalAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION).orElse(null);
        }
    }
}
