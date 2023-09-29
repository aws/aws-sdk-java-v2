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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class MultipartDownloadHelperTest {

    @Test
    void testMultipartGet() throws Exception {
        S3AsyncClient s3AsyncClient =
            S3AsyncClient.builder()
                         .credentialsProvider(ProfileCredentialsProvider.create())
                         .region(Region.EU_WEST_1)
                         .httpClient(NettyNioAsyncHttpClient.builder()
                                                            .maxConcurrency(1000)
                                                            .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                                                            .build())
                         .build();
        MultipartDownloadHelper<ResponseBytes<GetObjectResponse>> helper = new MultipartDownloadHelper<>(s3AsyncClient);
        GetObjectRequest req = GetObjectRequest.builder()
                                               .bucket("do-not-delete-crt-s3-eu-west-1")
                                               .key("512MB")
                                               .build();
        CompletableFuture<ResponseBytes<GetObjectResponse>> responseFuture =
            helper.getObject(req, AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> res = responseFuture.join();
        Thread.sleep(10000);
        System.out.println("response: " + res.response());
        System.out.println("bytes length: " + res.asByteArray().length);
        Thread.sleep(1000);
        System.out.println("All Done");
    }
}