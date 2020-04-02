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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedAbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedCreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

public class S3PresignerIntegrationTest {
    private static S3Client client;
    private static String testBucket;
    private static String testNonDnsCompatibleBucket;
    private static String testGetObjectKey;
    private static String testObjectContent;

    private S3Presigner presigner;

    @BeforeClass
    public static void setUpClass() {
        client = S3Client.create();
        testBucket = S3TestUtils.getTestBucket(client);
        testNonDnsCompatibleBucket = S3TestUtils.getNonDnsCompatibleTestBucket(client);
        testGetObjectKey = generateRandomObjectKey();
        testObjectContent = "Howdy!";

        S3TestUtils.putObject(S3PresignerIntegrationTest.class, client, testBucket, testGetObjectKey, testObjectContent);
        S3TestUtils.putObject(S3PresignerIntegrationTest.class, client, testNonDnsCompatibleBucket, testGetObjectKey, testObjectContent);
    }

    @AfterClass
    public static void tearDownClass() {
        S3TestUtils.runCleanupTasks(S3PresignerIntegrationTest.class);
        client.close();
    }

    private static String generateRandomObjectKey() {
        return "s3-presigner-it-" + UUID.randomUUID();
    }

    @Before
    public void setUpInstance() {
        this.presigner = S3Presigner.create();
    }

    @After
    public void testDownInstance() {
        this.presigner.close();
    }


    @Test
    public void browserCompatiblePresignedUrlWorks() throws IOException {
        assertThatPresigningWorks(testBucket, testGetObjectKey);
    }

    @Test
    public void bucketsWithScaryCharactersWorks() throws IOException {
        assertThatPresigningWorks(testNonDnsCompatibleBucket, testGetObjectKey);
    }

    @Test
    public void keysWithScaryCharactersWorks() throws IOException {
        String scaryObjectKey = testGetObjectKey + " !'/()~`";
        S3TestUtils.putObject(S3PresignerIntegrationTest.class, client, testBucket, scaryObjectKey, testObjectContent);

        assertThatPresigningWorks(testBucket, scaryObjectKey);
    }

    private void assertThatPresigningWorks(String bucket, String objectKey) throws IOException {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(gor -> gor.bucket(bucket).key(objectKey)));

        assertThat(presigned.isBrowserExecutable()).isTrue();

        try (InputStream response = presigned.url().openConnection().getInputStream()) {
            assertThat(IoUtils.toUtf8String(response)).isEqualTo(testObjectContent);
        }
    }

    @Test
    public void browserIncompatiblePresignedUrlDoesNotWorkWithoutAdditionalHeaders() throws IOException {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(gor -> gor.bucket(testBucket)
                                                                         .key(testGetObjectKey)
                                                                         .requestPayer(RequestPayer.REQUESTER)));

        assertThat(presigned.isBrowserExecutable()).isFalse();

        HttpURLConnection connection = (HttpURLConnection) presigned.url().openConnection();
        connection.connect();
        try {
            assertThat(connection.getResponseCode()).isEqualTo(403);
        } finally {
            connection.disconnect();
        }
    }

    @Test
    public void browserIncompatiblePresignedUrlWorksWithAdditionalHeaders() throws IOException {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(gor -> gor.bucket(testBucket)
                                                                         .key(testGetObjectKey)
                                                                         .requestPayer(RequestPayer.REQUESTER)));

        assertThat(presigned.isBrowserExecutable()).isFalse();

        HttpURLConnection connection = (HttpURLConnection) presigned.url().openConnection();

        presigned.httpRequest().headers().forEach((header, values) -> {
            values.forEach(value -> {
                connection.addRequestProperty(header, value);
            });
        });

        try (InputStream content = connection.getInputStream()) {
            assertThat(IoUtils.toUtf8String(content)).isEqualTo(testObjectContent);
        }
    }

    @Test
    public void getObject_PresignedHttpRequestCanBeInvokedDirectlyBySdk() throws IOException {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(gor -> gor.bucket(testBucket)
                                                                         .key(testGetObjectKey)
                                                                         .requestPayer(RequestPayer.REQUESTER)));

        assertThat(presigned.isBrowserExecutable()).isFalse();

        SdkHttpClient httpClient = ApacheHttpClient.builder().build(); // or UrlConnectionHttpClient.builder().build()

        ContentStreamProvider requestPayload = presigned.signedPayload()
                                                        .map(SdkBytes::asContentStreamProvider)
                                                        .orElse(null);

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(presigned.httpRequest())
                                                       .contentStreamProvider(requestPayload)
                                                       .build();

        HttpExecuteResponse response = httpClient.prepareRequest(request).call();

        assertThat(response.responseBody()).isPresent();
        try (InputStream responseStream = response.responseBody().get()) {
            assertThat(IoUtils.toUtf8String(responseStream)).isEqualTo(testObjectContent);
        }
    }

    @Test
    public void putObject_PresignedHttpRequestCanBeInvokedDirectlyBySdk() throws IOException {
        String objectKey = generateRandomObjectKey();
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.deleteObject(r -> r.bucket(testBucket).key(objectKey)));

        PresignedPutObjectRequest presigned =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .putObjectRequest(por -> por.bucket(testBucket).key(objectKey)));

        assertThat(presigned.isBrowserExecutable()).isFalse();

        SdkHttpClient httpClient = ApacheHttpClient.builder().build(); // or UrlConnectionHttpClient.builder().build()

        ContentStreamProvider requestPayload = () -> new StringInputStream(testObjectContent);

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(presigned.httpRequest())
                                                       .contentStreamProvider(requestPayload)
                                                       .build();

        HttpExecuteResponse response = httpClient.prepareRequest(request).call();

        assertThat(response.responseBody()).isPresent();
        assertThat(response.httpResponse().isSuccessful()).isTrue();
        response.responseBody().ifPresent(AbortableInputStream::abort);
        String content = client.getObjectAsBytes(r -> r.bucket(testBucket).key(objectKey)).asUtf8String();
        assertThat(content).isEqualTo(testObjectContent);
    }

    @Test
    public void createMultipartUpload_CanBePresigned() throws IOException {
        String objectKey = generateRandomObjectKey();

        PresignedCreateMultipartUploadRequest presigned =
            presigner.presignCreateMultipartUpload(p -> p.signatureDuration(Duration.ofMinutes(10))
                                                         .createMultipartUploadRequest(createMultipartUploadRequest(objectKey)));

        HttpExecuteResponse response = execute(presigned, null);

        assertThat(response.httpResponse().isSuccessful()).isTrue();

        Optional<MultipartUpload> upload = getMultipartUpload(objectKey);
        assertThat(upload).isPresent();

        client.abortMultipartUpload(abortMultipartUploadRequest(objectKey, upload.get().uploadId()));
    }

    @Test
    public void uploadPart_CanBePresigned() throws IOException {
        String objectKey = generateRandomObjectKey();
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.deleteObject(r -> r.bucket(testBucket).key(objectKey)));

        CreateMultipartUploadResponse create = client.createMultipartUpload(createMultipartUploadRequest(objectKey));
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.abortMultipartUpload(abortMultipartUploadRequest(objectKey, create.uploadId())));

        PresignedUploadPartRequest uploadPart =
            presigner.presignUploadPart(up -> up.signatureDuration(Duration.ofDays(1))
                                                .uploadPartRequest(upr -> upr.bucket(testBucket)
                                                                             .key(objectKey)
                                                                             .partNumber(1)
                                                                             .uploadId(create.uploadId())));


        HttpExecuteResponse uploadPartResponse = execute(uploadPart, testObjectContent);
        assertThat(uploadPartResponse.httpResponse().isSuccessful()).isTrue();
        String etag = uploadPartResponse.httpResponse().firstMatchingHeader("ETag").orElse(null);

        client.completeMultipartUpload(createMultipartUploadRequest(objectKey, create, etag));

        String content = client.getObjectAsBytes(r -> r.bucket(testBucket).key(objectKey)).asUtf8String();
        assertThat(content).isEqualTo(testObjectContent);
    }

    @Test
    public void completeMultipartUpload_CanBePresigned() throws IOException {
        String objectKey = generateRandomObjectKey();
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.deleteObject(r -> r.bucket(testBucket).key(objectKey)));

        CreateMultipartUploadResponse create = client.createMultipartUpload(createMultipartUploadRequest(objectKey));
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.abortMultipartUpload(abortMultipartUploadRequest(objectKey, create.uploadId())));

        UploadPartResponse uploadPartResponse = client.uploadPart(uploadPartRequest(objectKey, create),
                                                                  RequestBody.fromString(testObjectContent));
        String etag = uploadPartResponse.eTag();

        PresignedCompleteMultipartUploadRequest presignedRequest =
            presigner.presignCompleteMultipartUpload(
                r -> r.signatureDuration(Duration.ofDays(1))
                      .completeMultipartUploadRequest(createMultipartUploadRequest(objectKey, create, etag)));

        assertThat(execute(presignedRequest, presignedRequest.signedPayload().get().asUtf8String())
                       .httpResponse().isSuccessful()).isTrue();

        String content = client.getObjectAsBytes(r -> r.bucket(testBucket).key(objectKey)).asUtf8String();
        assertThat(content).isEqualTo(testObjectContent);
    }

    @Test
    public void abortMultipartUpload_CanBePresigned() throws IOException {
        String objectKey = generateRandomObjectKey();
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.deleteObject(r -> r.bucket(testBucket).key(objectKey)));

        CreateMultipartUploadResponse create = client.createMultipartUpload(createMultipartUploadRequest(objectKey));
        S3TestUtils.addCleanupTask(S3PresignerIntegrationTest.class,
                                   () -> client.abortMultipartUpload(abortMultipartUploadRequest(objectKey, create.uploadId())));

        PresignedAbortMultipartUploadRequest presignedRequest = presigner.presignAbortMultipartUpload(
            r -> r.signatureDuration(Duration.ofDays(1))
                  .abortMultipartUploadRequest(abortMultipartUploadRequest(objectKey, create.uploadId())));


        assertThat(execute(presignedRequest, null).httpResponse().isSuccessful()).isTrue();

        assertThat(getMultipartUpload(objectKey)).isNotPresent();
    }

    private Consumer<CreateMultipartUploadRequest.Builder> createMultipartUploadRequest(String objectKey) {
        return r -> r.bucket(testBucket).key(objectKey);
    }

    private Consumer<UploadPartRequest.Builder> uploadPartRequest(String objectKey, CreateMultipartUploadResponse create) {
        return r -> r.bucket(testBucket)
                     .key(objectKey)
                     .partNumber(1)
                     .uploadId(create.uploadId());
    }

    private Consumer<CompleteMultipartUploadRequest.Builder> createMultipartUploadRequest(String objectKey, CreateMultipartUploadResponse create, String etag) {
        return c -> c.bucket(testBucket)
                     .key(objectKey)
                     .uploadId(create.uploadId())
                     .multipartUpload(m -> m.parts(p -> p.partNumber(1).eTag(etag)));
    }

    private Consumer<AbortMultipartUploadRequest.Builder> abortMultipartUploadRequest(String objectKey, String uploadId) {
        return r -> r.bucket(testBucket)
                     .key(objectKey)
                     .uploadId(uploadId);
    }

    private Optional<MultipartUpload> getMultipartUpload(String objectKey) {
        return client.listMultipartUploadsPaginator(r -> r.bucket(testBucket).prefix(objectKey))
                     .uploads()
                     .stream()
                     .filter(u -> u.key().equals(objectKey))
                     .findAny();
    }

    private HttpExecuteResponse execute(PresignedRequest presigned, String payload) throws IOException {
        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        ContentStreamProvider requestPayload = payload == null ? null : () -> new StringInputStream(payload);

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(presigned.httpRequest())
                                                       .contentStreamProvider(requestPayload)
                                                       .build();

        return httpClient.prepareRequest(request).call();
    }
}
