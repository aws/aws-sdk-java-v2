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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

// WIP - please ignore
class MultipartDownloadIntegrationTest {

    static final String bucket = "olapplin-test-bucket";
    static final String key = "debug-test-24mb";

    private S3AsyncClient s3;

    @BeforeEach
    void init() {
        this.s3 = S3AsyncClient.builder()
                               .region(Region.US_WEST_2)
                               .credentialsProvider(ProfileCredentialsProvider.create())
                               .httpClient(NettyNioAsyncHttpClient.create())
                               .build();
    }

    // @Test
    void testByteAsyncResponseTransformer() {
        AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> transformer =
            AsyncResponseTransformer.toBytes();
        DownloaderSubscriber downloaderSubscriber = new DownloaderSubscriber(s3, GetObjectRequest.builder()
                                                                                                 .bucket(bucket).key(key)
                                                                                                 .build());
        CompletableFuture<ResponseBytes<GetObjectResponse>> future = new CompletableFuture<>();

        transformer.split(1024 * 1024 * 32, future).subscribe(downloaderSubscriber);
        ResponseBytes<GetObjectResponse> res = future.join();
        System.out.println("[Test] complete");
        try {
            // System.out.printf("[Test] result: %s%n", res);
            byte[] bytes = res.asByteArray();
            System.out.printf("[Test] Byte len: %s%n", bytes.length);
            assertThat(bytes.length / (1024 * 1024)).isEqualTo(24);
            System.out.println("[Test] all done");
        } catch (Exception err) {
            System.out.println(err);
            fail(err);
        }
    }
}
