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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PresignedDownloadRequest;
import software.amazon.awssdk.services.s3.model.PresignedDownloadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.testutils.RandomTempFile;

public class PresignedDownloadIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(GetObjectIntegrationTest.class);

    private static final String KEY = "some-key";

    private static File file;

    private static byte[] fileBytes;

    private static S3AsyncClient s3ClientWithoutCreds;

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        file = new RandomTempFile(10_000);
        fileBytes = Files.readAllBytes(file.toPath());
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), file.toPath());
        s3ClientWithoutCreds = S3AsyncClient.builder()
                                            .credentialsProvider(() -> null)
                                            .build();;
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
        file.delete();
    }

    @Test
    public void presignedDownload_toFile_downloadsCorrectly() throws IOException {
        PresignedDownloadRequest request = presignedDownloadRequest();

        Path path = RandomTempFile.randomUncreatedFile().toPath();
        PresignedDownloadResponse response = null;

        try {
            response = s3ClientWithoutCreds.presignedDownload(request, path).join();
        } finally {
            assertEquals(Long.valueOf(file.length()), response.contentLength());
            byte[] downloadedBytes = Files.readAllBytes(path);
            assertArrayEquals(downloadedBytes, fileBytes);
            path.toFile().delete();
        }
    }

    @Test
    public void presignedDownload_toInputStream_downloadsCorrectly() throws IOException {
        PresignedDownloadRequest request = presignedDownloadRequest();

        ResponseInputStream<PresignedDownloadResponse> object = null;

        try {
            object = s3Async.presignedDownload(request, AsyncResponseTransformer.toBlockingInputStream()).join();
        } finally {
            Long contentLength = object.response().contentLength();
            assertEquals(Long.valueOf(file.length()), contentLength);
            byte[] buffer = new byte[Math.toIntExact(contentLength)];
            object.read(buffer);
            assertArrayEquals(buffer, fileBytes);
        }
    }

    private PresignedDownloadRequest presignedDownloadRequest() {
        URL presignedUrl = generatePresignedGetObjectUrl();
        return PresignedDownloadRequest.builder().presignedUrl(presignedUrl).build();
    }

    private URL generatePresignedGetObjectUrl() {
        GetObjectRequest getObjectRequest = getObjectRequest();
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                                                                                 .getObjectRequest(getObjectRequest)
                                                                                 .signatureDuration(Duration.ofHours(1))
                                                                                 .build();
        S3Presigner presigner = S3Presigner.create();
        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url();
    }

    private GetObjectRequest getObjectRequest() {
        return GetObjectRequest.builder()
                               .bucket(BUCKET)
                               .key(KEY)
                               .build();
    }
}
