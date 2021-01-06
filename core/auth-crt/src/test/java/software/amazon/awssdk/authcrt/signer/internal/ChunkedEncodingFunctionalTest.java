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

package software.amazon.awssdk.authcrt.signer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.authcrt.signer.internal.SigningUtils.SIGNING_CLOCK;
import static software.amazon.awssdk.authcrt.signer.internal.SigningUtils.buildCredentials;
import static software.amazon.awssdk.authcrt.signer.internal.SigningUtils.getSigningClock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingConfig;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.authcrt.signer.internal.chunkedencoding.AwsS3V4aChunkSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

/**
 * Functional tests for the S3 specific Sigv4a signer. These tests call the CRT native signer code. Because
 * Sigv4 Asymmetric does not yield deterministic results, the signatures can only be verified by using
 * a pre-calculated test case and calling a verification method with information from that test case.
 */
@RunWith(MockitoJUnitRunner.class)
public class ChunkedEncodingFunctionalTest {

    private static final String CHUNKED_SIGV4A_CANONICAL_REQUEST = "PUT\n" +
                                                                   "/examplebucket/chunkObject.txt\n" +
                                                                   "\n" +
                                                                   "content-encoding:aws-chunked\n" +
                                                                   "content-length:66824\n" +
                                                                   "host:s3.amazonaws.com\n" +
                                                                   "x-amz-content-sha256:STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD\n" +
                                                                   "x-amz-date:20130524T000000Z\n" +
                                                                   "x-amz-decoded-content-length:66560\n" +
                                                                   "x-amz-region-set:us-east-1\n" +
                                                                   "x-amz-storage-class:REDUCED_REDUNDANCY\n" +
                                                                   "\n" +
                                                                   "content-encoding;content-length;host;x-amz-content-sha256;x-amz-date;x-amz-decoded-content-length;x-amz-region-set;x-amz-storage-class\n" +
                                                                   "STREAMING-AWS4-ECDSA-P256-SHA256-PAYLOAD";

    private static final String CHUNKED_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
    private static final String CHUNKED_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final String CHUNKED_SIGV4A_TEST_ECC_PUB_X = "18b7d04643359f6ec270dcbab8dce6d169d66ddc9778c75cfb08dfdb701637ab";
    private static final String CHUNKED_SIGV4A_TEST_ECC_PUB_Y = "fa36b35e4fe67e3112261d2e17a956ef85b06e44712d2850bcd3c2161e9993f2";
    private static final String CHUNKED_TEST_REGION = "us-east-1";
    private static final String CHUNKED_TEST_SERVICE = "s3";
    private static final String CHUNKED_TEST_SIGNING_TIME = "2013-05-24T00:00:00Z";

    private static final String CHUNK_STS_PRE_SIGNATURE = "AWS4-ECDSA-P256-SHA256-PAYLOAD\n" +
                                                          "20130524T000000Z\n" +
                                                          "20130524/s3/aws4_request\n";

    private static final String CHUNK1_STS_POST_SIGNATURE = "\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                                                            "bf718b6f653bebc184e1479f1935b8da974d701b893afcf49e701f3e2f9f9c5a";

    private static final String CHUNK2_STS_POST_SIGNATURE = "\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                                                            "2edc986847e209b4016e141a6dc8716d3207350f416969382d431539bf292e4a";

    private static final String CHUNK3_STS_POST_SIGNATURE = "\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                                                            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final String CRLF = "\r\n";
    private static final String SIGNATURE_KEY = "chunk-signature=";

    private static final int DATA_SIZE = 66560;
    /**
     * This content-length is actually incorrect; the correct calculated length should be 67064. However, since
     * the test case was developed using this number, it must be used when calculating the signatures or else
     * the test will fail verification. This is also the reason the sigv4a signer cannot be called directly in
     * a test since it would calculate a different content length.
     */
    private static final int TOTAL_CONTENT_LENGTH = 66824;
    private static final int STREAM_CHUNK_SIZE = 65536;
    private static final int CHUNK2_SIZE = 1024;
    private static final byte[] data;

    private static final SimpleDateFormat DATE_FORMAT;

    @Mock
    SigningConfigProvider configProvider;

    CrtHttpRequestConverter converter;
    AwsCrt4aSigningAdapter adapter;
    AwsSigningConfig chunkedRequestSigningConfig;
    AwsSigningConfig chunkSigningConfig;
    ExecutionAttributes executionAttributes;

    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        data = new byte[DATA_SIZE];
        Arrays.fill(data, (byte) 'a');
    }

    @Before
    public void setup() throws Exception {
        converter = new CrtHttpRequestConverter();
        adapter = new AwsCrt4aSigningAdapter();
        executionAttributes = buildBasicExecutionAttributes();
        chunkedRequestSigningConfig = createChunkedRequestSigningConfig(executionAttributes);
        chunkSigningConfig = createChunkSigningConfig(executionAttributes);
        when(configProvider.createS3CrtSigningConfig(any())).thenReturn(chunkedRequestSigningConfig);
        when(configProvider.createChunkedSigningConfig(any())).thenReturn(createChunkSigningConfig(buildBasicExecutionAttributes()));
    }

    @Test
    public void calling_adapter_APIs_directly_creates_correct_signatures() {
        byte[] previousSignature = createAndVerifyRequestSignature(defaultHttpRequest().build());

        for (int i = 0; i < 3; i++) {
            byte[] currentSignature = adapter.signChunk(getChunkData(i), previousSignature, chunkSigningConfig);
            assertTrue(AwsSigningUtils.verifyRawSha256EcdsaSignature(createStringToSign(i, previousSignature),
                                                                     currentSignature,
                                                                     CHUNKED_SIGV4A_TEST_ECC_PUB_X,
                                                                     CHUNKED_SIGV4A_TEST_ECC_PUB_Y));
            previousSignature = currentSignature;
        }
    }

    @Test
    public void using_a_request_stream_creates_correct_signatures() throws Exception {
        SdkHttpFullRequest request = defaultHttpRequest()
            .contentStreamProvider(() -> new ByteArrayInputStream(data))
            .build();
        byte[] requestSignature = createAndVerifyRequestSignature(request);

        AwsS3V4aChunkSigner chunkSigner = new AwsS3V4aChunkSigner(adapter, chunkSigningConfig);
        AwsChunkedEncodingConfig chunkedEncodingConfig = AwsChunkedEncodingConfig.builder()
                                                                                 .chunkSize(STREAM_CHUNK_SIZE)
                                                                                 .build();
        AwsChunkedEncodingInputStream stream =
            new AwsChunkedEncodingInputStream(request.contentStreamProvider().get().newStream(),
                                              new String(requestSignature, StandardCharsets.UTF_8),
                                              chunkSigner,
                                              chunkedEncodingConfig);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        String result = new String(output.toByteArray(), StandardCharsets.UTF_8);

        assertChunks(result, 3, requestSignature);
    }

    private void assertChunks(String result, int numExpectedChunks, byte[] requestSignature) {
        List<String> lines = Stream.of(result.split(CRLF)).collect(Collectors.toList());
        assertThat(lines.size()).isEqualTo(numExpectedChunks * 2 - 1);
        byte[] previousSignature = requestSignature;
        for (String line : lines) {
            int chunk = lines.indexOf(line) / 2;
            if (lines.indexOf(line) % 2 == 0) {
                String signatureValue = line.substring(line.indexOf(SIGNATURE_KEY) + SIGNATURE_KEY.length());
                byte[] currentSignature = signatureValue.getBytes(StandardCharsets.UTF_8);
                assertThat(signatureValue.length()).isEqualTo(AwsS3V4aChunkSigner.getSignatureLength());
                assertTrue(AwsSigningUtils.verifyRawSha256EcdsaSignature(createStringToSign(chunk, previousSignature),
                                                                         currentSignature,
                                                                         CHUNKED_SIGV4A_TEST_ECC_PUB_X,
                                                                         CHUNKED_SIGV4A_TEST_ECC_PUB_Y));
                previousSignature = currentSignature;
            }
        }
    }

    private byte[] createAndVerifyRequestSignature(SdkHttpFullRequest request) {
        SdkSigningResult result = adapter.sign(request, chunkedRequestSigningConfig);
        byte[] requestSignature = result.getSignature();
        assertTrue(AwsSigningUtils.verifySigv4aEcdsaSignature(converter.requestToCrt(request),
                                                              CHUNKED_SIGV4A_CANONICAL_REQUEST,
                                                              chunkedRequestSigningConfig,
                                                              requestSignature,
                                                              CHUNKED_SIGV4A_TEST_ECC_PUB_X,
                                                              CHUNKED_SIGV4A_TEST_ECC_PUB_Y));
        return requestSignature;
    }

    private ExecutionAttributes buildBasicExecutionAttributes() throws ParseException {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SIGNING_CLOCK, Clock.fixed(DATE_FORMAT.parse(CHUNKED_TEST_SIGNING_TIME).toInstant(), ZoneId.systemDefault()));
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, CHUNKED_TEST_SERVICE);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, Region.of(CHUNKED_TEST_REGION));
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                                         AwsBasicCredentials.create(CHUNKED_ACCESS_KEY_ID, CHUNKED_SECRET_ACCESS_KEY));
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, false);
        return executionAttributes;
    }

    private AwsSigningConfig createChunkedRequestSigningConfig(ExecutionAttributes executionAttributes) throws Exception {
        AwsSigningConfig config = createBasicSigningConfig(executionAttributes);

        config.setUseDoubleUriEncode(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
        config.setShouldNormalizeUriPath(true);

        config.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);
        config.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_VIA_HEADERS);

        config.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.STREAMING_AWS4_ECDSA_P256_SHA256_PAYLOAD);

        return config;
    }

    private AwsSigningConfig createChunkSigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig config = createBasicSigningConfig(executionAttributes);

        config.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.NONE);
        config.setSignatureType(AwsSigningConfig.AwsSignatureType.HTTP_REQUEST_CHUNK);

        return config;
    }

    private AwsSigningConfig createBasicSigningConfig(ExecutionAttributes executionAttributes) {
        AwsSigningConfig config = new AwsSigningConfig();
        config.setCredentials(buildCredentials(executionAttributes));
        config.setService(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME));
        config.setRegion(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION).id());
        config.setTime(getSigningClock(executionAttributes).instant().toEpochMilli());
        config.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
        return config;
    }

    private SdkHttpFullRequest.Builder defaultHttpRequest() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.PUT)
                                 .putHeader("x-amz-storage-class", "REDUCED_REDUNDANCY")
                                 .putHeader("Content-Encoding", "aws-chunked")
                                 .uri(URI.create("https://s3.amazonaws.com/examplebucket/chunkObject.txt"))
                                 .putHeader("x-amz-decoded-content-length", Integer.toString(DATA_SIZE))
                                 .putHeader("Content-Length", Integer.toString(TOTAL_CONTENT_LENGTH));
    }

    private byte[] getChunkData(int chunk) {
        switch(chunk) {
            case 0: return Arrays.copyOfRange(data, 0, STREAM_CHUNK_SIZE);
            case 1: return Arrays.copyOfRange(data, STREAM_CHUNK_SIZE, STREAM_CHUNK_SIZE + CHUNK2_SIZE);
            default: return new byte[0];
        }
    }

    private byte[] createStringToSign(int chunk, byte[] previousSignature) {
        switch(chunk) {
            case 0: return buildChunkStringToSign(previousSignature, CHUNK1_STS_POST_SIGNATURE);
            case 1: return buildChunkStringToSign(previousSignature, CHUNK2_STS_POST_SIGNATURE);
            default: return buildChunkStringToSign(previousSignature, CHUNK3_STS_POST_SIGNATURE);
        }
    }

    private byte[] buildChunkStringToSign(byte[] previousSignature, String stsPostSignature) {
        StringBuilder stsBuilder = new StringBuilder();

        stsBuilder.append(CHUNK_STS_PRE_SIGNATURE);
        String signature = new String(previousSignature, StandardCharsets.UTF_8);
        int paddingIndex = signature.indexOf('*');
        if (paddingIndex != -1) {
            signature = signature.substring(0, paddingIndex);
        }
        stsBuilder.append(signature);
        stsBuilder.append(stsPostSignature);

        return stsBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
