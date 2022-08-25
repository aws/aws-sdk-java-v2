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

package software.amazon.awssdk.services.s3.checksum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.internal.DefaultAwsCrtS3V4aSigner;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.utils.CaptureChecksumValidationInterceptor;
import software.amazon.awssdk.testutils.Waiter;

public class HttpChecksumIntegrationTest extends S3IntegrationTestBase {

    protected static final String KEY = "some-key";
    public static final int HUGE_MSG_SIZE = 16384;
    public static CaptureChecksumValidationInterceptor interceptor = new CaptureChecksumValidationInterceptor();
    protected static S3Client s3Https;
    protected static S3AsyncClient s3HttpAsync;
    private static String BUCKET = temporaryBucketName(HttpChecksumIntegrationTest.class);

    @BeforeAll
    public static void setUp() throws Exception {

        // Http Client to generate Signed request
        s3 = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                              .endpointOverride(URI.create("http://s3." + DEFAULT_REGION + ".amazonaws.com")).build();

        s3Async = s3AsyncClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)).build();

        s3Https = s3ClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)).build();

        // Http Client to generate Signed request
        s3HttpAsync = s3AsyncClientBuilder().overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                            .endpointOverride(URI.create("http://s3." + DEFAULT_REGION + ".amazonaws.com")).build();


        createBucket(BUCKET);

        Waiter.run(() -> s3.headBucket(r -> r.bucket(BUCKET)))
              .ignoringException(NoSuchBucketException.class)
              .orFail();
        interceptor.reset();
    }

    @AfterAll
    public static void tearDown(){
        deleteBucketAndAllContents(BUCKET);
    }

    private static String createDataSize(int msgSize) {
        msgSize = msgSize / 2;
        msgSize = msgSize * 1024;
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i = 0; i < msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @AfterEach
    public void clear() {
        interceptor.reset();
    }

    @Test
    public void validHeaderChecksumCalculatedBySdkClient() {
        PutObjectResponse putObjectResponse = s3Https.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                .key(KEY)
                                                                                .build(), RequestBody.fromString("Hello world"));
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        assertThat(putObjectResponse.sdkHttpResponse().firstMatchingHeader("x-amz-checksum-crc32"))
            .hasValue("i9aeUg==");
    }

    @Test
    public void validHeaderChecksumSentDirectlyInTheField() {
        PutObjectResponse putObjectResponse = s3Https.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                .checksumCRC32("i9aeUg==")
                                                                                .key(KEY)
                                                                                .build(), RequestBody.fromString("Hello world"));
        assertThat(interceptor.requestChecksumInHeader()).isEqualTo("i9aeUg==");
        assertThat(interceptor.requestChecksumInTrailer()).isNull();
        assertThat(putObjectResponse.sdkHttpResponse().firstMatchingHeader("x-amz-checksum-crc32")).hasValue("i9aeUg==");
    }

    @Test
    public void validHeaderChecksumSentDirectlyInTheFieldAndFeatureEnabled() {
        PutObjectResponse putObjectResponse = s3Https.putObject(PutObjectRequest.builder()
                                                                                .bucket(BUCKET)
                                                                                .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                                                                .checksumCRC32("i9aeUg==")
                                                                                .key(KEY)
                                                                                .build(), RequestBody.fromString("Hello world"));
        assertThat(interceptor.requestChecksumInHeader()).isEqualTo("i9aeUg==");
        assertThat(interceptor.requestChecksumInTrailer()).isNull();
        assertThat(putObjectResponse.sdkHttpResponse().firstMatchingHeader("x-amz-checksum-crc32")).hasValue("i9aeUg==");
    }

    @Test
    public void invalidHeaderChecksumCalculatedByUserNotOverWrittenBySdkClient() {
        assertThatExceptionOfType(S3Exception.class).isThrownBy(
                                                        () -> s3Https.putObject(PutObjectRequest.builder()
                                                                                                .bucket(BUCKET)
                                                                                                .checksumCRC32("i9aeUgg=")
                                                                                                .key(KEY)
                                                                                                .build(),
                                                                                RequestBody.fromString("Hello world")))
                                                    .withMessageContaining("Value for x-amz-checksum-crc32 header is invalid");
    }

    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient() throws InterruptedException {
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3Https.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");
    }


    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClient() {


        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder()
                                         .bucket(BUCKET)
                                         .key(KEY)
                                         .checksumMode(ChecksumMode.ENABLED)
                                         .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");

    }

    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClient_Empty_String() {


        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), RequestBody.fromString(""));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("");

    }

    @Test
    public void syncValidSignedTrailerChecksumCalculatedBySdkClientWithSigv4a() {

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .overrideConfiguration(o -> o.signer(DefaultAwsCrtS3V4aSigner.create()))
                                     .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");
    }

    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClientWithSigv4a() {

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .overrideConfiguration(o -> o.signer(DefaultAwsCrtS3V4aSigner.create()))
                                     .build(), RequestBody.fromString("Hello world"));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo("Hello world");
    }


    @Test
    public void asyncValidUnsignedTrailerChecksumCalculatedBySdkClient() throws InterruptedException {
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .overrideConfiguration(o -> o.signer(DefaultAwsCrtS3V4aSigner.create()))

                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromString("Hello world")).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo("Hello world");
    }

    @Test
    public void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withSmallRequestBody() throws InterruptedException {
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromString("Hello world")).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo("Hello world");
    }

    @Test
    public void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withHugeRequestBody() throws InterruptedException {
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromString(createDataSize(HUGE_MSG_SIZE))).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(response).isEqualTo(createDataSize(HUGE_MSG_SIZE));
    }


    @Disabled("Http Async Signing is not supported for S3")
    public void asyncValidSignedTrailerChecksumCalculatedBySdkClient() {
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder()
                                                                     .put(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING,
                                                                          true).build();
        s3HttpAsync.putObject(PutObjectRequest.builder()
                                              .bucket(BUCKET)
                                              .overrideConfiguration(o -> o.executionAttributes(executionAttributes))
                                              .key(KEY)
                                              .build(), AsyncRequestBody.fromString("Hello world")).join();
        String response = s3HttpAsync.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                                .key(KEY)
                                                                .build(), AsyncResponseTransformer.toBytes()).join()
                                     .asUtf8String();
        assertThat(response).isEqualTo("Hello world");
    }

    @Test
    public void syncUnsignedPayloadForHugeMessage() throws InterruptedException {
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromString(createDataSize(HUGE_MSG_SIZE)));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        Thread.sleep(1000);
        ResponseInputStream<GetObjectResponse> s3HttpsObject = s3Https.getObject(
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .checksumMode(ChecksumMode.ENABLED)
                            .key(KEY)
                            .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo(createDataSize(HUGE_MSG_SIZE));
    }

    @Test
    public void syncSignedPayloadForHugeMessage(){
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                     .build(), RequestBody.fromString(createDataSize(HUGE_MSG_SIZE)));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject = s3.getObject(
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .checksumMode(ChecksumMode.ENABLED)
                            .key(KEY)
                            .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        assertThat(text).isEqualTo(createDataSize(HUGE_MSG_SIZE));
    }

    @Test
    public void syncUnsignedPayloadMultiPartForHugeMessage() throws InterruptedException {
        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromString(createDataSize(HUGE_MSG_SIZE)));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();
        ResponseInputStream<GetObjectResponse> s3HttpsObject = s3Https.getObject(
            GetObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

        assertThat(interceptor.validationAlgorithm()).isNull();
        assertThat(interceptor.responseValidation()).isNull();
        assertThat(text).isEqualTo(createDataSize(HUGE_MSG_SIZE));
    }


    @Test
    public void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withSmallFileRequestBody() throws InterruptedException, IOException {
        File randomFileOfFixedLength = getRandomFileOfFixedLength(10);
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromFile(randomFileOfFixedLength.toPath())).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);

        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(response).isEqualTo(new String (bytes));


    }

    @Test
    public void asyncHttpsValidUnsignedTrailerChecksumCalculatedBySdkClient_withHugeFileRequestBody()
        throws IOException {

        File randomFileOfFixedLength = getRandomFileOfFixedLength(17);
        s3Async.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), AsyncRequestBody.fromFile(randomFileOfFixedLength.toPath())).join();
        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        String response = s3Async.getObject(GetObjectRequest.builder().bucket(BUCKET)
                                                            .key(KEY).checksumMode(ChecksumMode.ENABLED)
                                                            .build(), AsyncResponseTransformer.toBytes()).join().asUtf8String();
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);

        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(response).isEqualTo(new String (bytes));

    }

    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient_withSmallFileRequestBody() throws InterruptedException,
                                                                                                        IOException {

        File randomFileOfFixedLength = getRandomFileOfFixedLength(10);

        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromFile(randomFileOfFixedLength.toPath()));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3Https.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(text).isEqualTo(new String(bytes));
    }


    @Test
    public void syncValidUnsignedTrailerChecksumCalculatedBySdkClient_withHugeFileRequestBody() throws InterruptedException,
                                                                                                        IOException {

        File randomFileOfFixedLength = getRandomFileOfFixedLength(34);

        s3Https.putObject(PutObjectRequest.builder()
                                          .bucket(BUCKET)
                                          .key(KEY)
                                          .checksumAlgorithm(ChecksumAlgorithm.CRC32)
                                          .build(), RequestBody.fromFile(randomFileOfFixedLength.toPath()));

        assertThat(interceptor.requestChecksumInTrailer()).isEqualTo("x-amz-checksum-crc32");
        assertThat(interceptor.requestChecksumInHeader()).isNull();

        ResponseInputStream<GetObjectResponse> s3HttpsObject =
            s3Https.getObject(GetObjectRequest.builder().bucket(BUCKET).key(KEY).checksumMode(ChecksumMode.ENABLED).build());
        String text = new BufferedReader(
            new InputStreamReader(s3HttpsObject, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));
        assertThat(interceptor.validationAlgorithm()).isEqualTo(Algorithm.CRC32);
        assertThat(interceptor.responseValidation()).isEqualTo(ChecksumValidation.VALIDATED);
        byte[] bytes = Files.readAllBytes(randomFileOfFixedLength.toPath());
        assertThat(text).isEqualTo(new String(bytes));
    }

    private File getRandomFileOfFixedLength(int sizeInKb) throws IOException {
        int objectSize = sizeInKb * 1024  ;
        final File tempFile = File.createTempFile("s3-object-file-", ".tmp");
        try (RandomAccessFile f = new RandomAccessFile(tempFile, "rw")) {
            f.setLength(objectSize  );
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

}
