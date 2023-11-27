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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.InputStreamUtils;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;

class MultipartDownloaderIntegTest {

    private static final String FILE_PATH = "/Users/olapplin/Develop/tmp";

    static int MB_LEN_512 = 536870912;
    static String KEY_512 = "512MB";

    static int MB_LEN_32 = 33554432;
    static String KEY_32 = "mpu-get-32mb";

    static int LEN = MB_LEN_32;
    static String KEY = KEY_32;

    int maxMemBufferSize = 64 * 1024 * 1024;
    S3AsyncClient s3AsyncClient;

    @Test
    void testMultipartGetBlockingInputStream() {
        testMultipartGetBlockingInputStream(LEN, KEY);
    }

    @Test
    void testMultipartGetPublisher() {
        testMultipartGetPublisher(LEN, KEY);
    }

    @Test
    void testMultipartGetByte() {
        testMultipartGetByte(LEN, KEY);
    }

    @Test
    void testMultipartGetFile() {
        testMultipartGetFile(KEY);
    }

    @BeforeEach
    void init() {
        s3AsyncClient = S3AsyncClient.builder()
                                     .credentialsProvider(ProfileCredentialsProvider.create())
                                     .region(Region.EU_WEST_1)
                                     .httpClient(NettyNioAsyncHttpClient.builder()
                                                                        // .maxConcurrency(10)
                                                                        // .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                                                                        .build())
                                     .build();
    }

    void testMultipartGetByte(int len, String key) {
        MultipartDownloader<ResponseBytes<GetObjectResponse>> helper = new MultipartDownloader<>(s3AsyncClient, maxMemBufferSize);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("do-not-delete-crt-s3-eu-west-1")
                                               .key("512MB")
                                               .key(key)
                                               .build();
        CompletableFuture<ResponseBytes<GetObjectResponse>> responseFuture =
            helper.getObject(req, AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> res = responseFuture.join();
        System.out.println("response: " + res.response());
        byte[] bytes = res.asByteArray();
        System.out.println("bytes length: " + bytes.length);
        System.out.println("byte diff: " + (len - bytes.length));
        assertThat(len - bytes.length).isZero();
    }

    void testMultipartGetFile(String key) {
        Path path = Paths.get(FILE_PATH, key);
        // if (Files.exists(path)) {
        //     path.toFile().delete();
        // }
        MultipartDownloader<GetObjectResponse> helper =
            new MultipartDownloader<>(s3AsyncClient, maxMemBufferSize);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("do-not-delete-crt-s3-eu-west-1")
                                               .key("512MB")
                                               .key(key)
                                               .build();
        CompletableFuture<GetObjectResponse> responseFuture =
            helper.getObject(req, AsyncResponseTransformer.toFile(path));
        GetObjectResponse res = responseFuture.join();
        System.out.println("response: " + res);
    }

    void testMultipartGetBlockingInputStream(int len, String key) {
        MultipartDownloader<ResponseInputStream<GetObjectResponse>> helper = new MultipartDownloader<>(s3AsyncClient, maxMemBufferSize);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("do-not-delete-crt-s3-eu-west-1")
                                               .key(key)
                                               .key(key)
                                               .build();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> responseFuture =
            helper.getObject(req, AsyncResponseTransformer.toBlockingInputStream());
        ResponseInputStream<GetObjectResponse> res = responseFuture.join();
        System.out.println("response: " + res.response());
        byte[] bytes = InputStreamUtils.drainInputStream(res);
        System.out.println("bytes length: " + bytes.length);
        System.out.println("byte diff: " + (len - bytes.length));
        assertThat(len - bytes.length).isZero();
    }

    void testMultipartGetPublisher(int len, String key) {
        MultipartDownloader<ResponsePublisher<GetObjectResponse>> helper = new MultipartDownloader<>(s3AsyncClient, maxMemBufferSize);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("do-not-delete-crt-s3-eu-west-1")
                                               .key(key)
                                               .key(key)
                                               .build();
        CompletableFuture<ResponsePublisher<GetObjectResponse>> responseFuture =
            helper.getObject(req, AsyncResponseTransformer.toPublisher());
        ResponsePublisher<GetObjectResponse> res = responseFuture.join();
        System.out.println("response: " + res.response());

        ByteBufferStoringSubscriber sub = new ByteBufferStoringSubscriber(Long.MAX_VALUE);
        res.subscribe(sub);
        ByteBuffer bb = ByteBuffer.allocate(len);
        sub.transferTo(bb);
        byte[] bytes = bb.array();
        System.out.println("bytes length: " + bytes.length);
        System.out.println("byte diff: " + (len - bytes.length));
        assertThat(len - bytes.length).isZero();
    }

}