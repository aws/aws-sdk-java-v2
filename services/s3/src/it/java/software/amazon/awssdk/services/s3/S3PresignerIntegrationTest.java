/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;
import software.amazon.awssdk.utils.IoUtils;

public class S3PresignerIntegrationTest {
    private static S3Client client;
    private static String testBucket;
    private static String testNonDnsCompatibleBucket;
    private static String testObjectKey;
    private static String testObjectContent;

    private S3Presigner presigner;

    @BeforeClass
    public static void setUpClass() {
        client = S3Client.create();
        testBucket = S3TestUtils.getTestBucket(client);
        testNonDnsCompatibleBucket = S3TestUtils.getNonDnsCompatibleTestBucket(client);
        testObjectKey = "s3-presigner-it-" + UUID.randomUUID();
        testObjectContent = "Howdy!";

        S3TestUtils.putObject(S3PresignerIntegrationTest.class, client, testBucket, testObjectKey, testObjectContent);
        S3TestUtils.putObject(S3PresignerIntegrationTest.class, client, testNonDnsCompatibleBucket, testObjectKey, testObjectContent);
    }

    @AfterClass
    public static void tearDownClass() {
        S3TestUtils.runCleanupTasks(S3PresignerIntegrationTest.class);
        client.close();
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
        assertThatPresigningWorks(testBucket, testObjectKey);
    }

    @Test
    public void bucketsWithScaryCharactersWorks() throws IOException {
        assertThatPresigningWorks(testNonDnsCompatibleBucket, testObjectKey);
    }

    @Test
    public void keysWithScaryCharactersWorks() throws IOException {
        String scaryObjectKey = testObjectKey + " !'/()~`";
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
                                                                         .key(testObjectKey)
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
                                                                         .key(testObjectKey)
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
    public void presignedHttpRequestCanBeInvokedDirectlyBySdk() throws IOException {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(gor -> gor.bucket(testBucket)
                                                                         .key(testObjectKey)
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
}
