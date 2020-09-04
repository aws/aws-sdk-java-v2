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

package software.amazon.awssdk.http.crt;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;


public class AwsCrtClientS3IntegrationTest {
    /**
     * The name of the bucket created, used, and deleted by these tests.
     */
    private static String BUCKET_NAME = "aws-crt-test-stuff";

    private static String LARGE_FILE = "http_test_doc.txt";
    private static String SMALL_FILE = "random_32_byte.data";
    private static String LARGE_FILE_SHA256 = "C7FDB5314B9742467B16BD5EA2F8012190B5E2C44A005F7984F89AAB58219534";
    private static int NUM_REQUESTS = 1000;

    private static Region REGION = Region.US_EAST_1;

    private static SdkAsyncHttpClient crtClient;

    private static S3AsyncClient s3;

    @BeforeClass
    public static void setup() {
        CrtResource.waitForNoResources();

        crtClient = AwsCrtAsyncHttpClient.create();

        s3 = S3AsyncClient.builder()
                .region(REGION)
                .httpClient(crtClient)
                .credentialsProvider(AnonymousCredentialsProvider.create()) // File is publicly readable
                .build();
    }

    @AfterClass
    public static void tearDown() {
        s3.close();
        crtClient.close();
        CrtResource.waitForNoResources();
    }

    @Test
    public void testDownloadFromS3() throws Exception {
        GetObjectRequest s3Request = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(LARGE_FILE)
                .build();

        byte[] responseBody = s3.getObject(s3Request, AsyncResponseTransformer.toBytes()).get(120, TimeUnit.SECONDS).asByteArray();

        assertThat(sha256Hex(responseBody).toUpperCase()).isEqualTo(LARGE_FILE_SHA256);
    }

    @Test
    public void testParallelDownloadFromS3() throws Exception {
        List<CompletableFuture<ResponseBytes<GetObjectResponse>> > requestFutures = new ArrayList<>();

        for (int i = 0; i < NUM_REQUESTS; i++) {
            GetObjectRequest s3Request = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(SMALL_FILE)
                    .build();
            CompletableFuture<ResponseBytes<GetObjectResponse>> requestFuture = s3.getObject(s3Request, AsyncResponseTransformer.toBytes());
            requestFutures.add(requestFuture);
        }

        for(CompletableFuture<ResponseBytes<GetObjectResponse>>  f: requestFutures) {
            f.join();
            Assert.assertEquals(32, f.get().asByteArray().length);
        }
    }

}
