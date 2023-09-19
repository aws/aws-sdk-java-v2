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

package software.amazon.awssdk.services.mediastoredata;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.mediastoredata.model.DeleteObjectRequest;
import software.amazon.awssdk.services.mediastoredata.model.ObjectNotFoundException;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectRequest;
import software.amazon.awssdk.testutils.Waiter;

/**
 * Integration test to verify Transfer-Encoding:chunked functionalities for all supported HTTP clients. Do not delete.
 */
public class TransferEncodingChunkedIntegrationTest extends MediaStoreDataIntegrationTestBase {
    protected static final String CONTAINER_NAME = "java-sdk-test-mediastoredata-transferencoding" + Instant.now().toEpochMilli();
    private static MediaStoreDataClient syncClientWithApache;
    private static MediaStoreDataClient syncClientWithUrlConnection;
    private static MediaStoreDataAsyncClient asyncClientWithNetty;
    private static PutObjectRequest putObjectRequest;
    private static DeleteObjectRequest deleteObjectRequest;

    @BeforeAll
    public static void setup() {
        uri = URI.create(createContainer(CONTAINER_NAME).endpoint());
        syncClientWithApache = MediaStoreDataClient.builder()
                                                   .endpointOverride(uri)
                                                   .credentialsProvider(credentialsProvider)
                                                   .httpClient(ApacheHttpClient.builder().build())
                                                   .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureTransferEncodingHeaderInterceptor()))
                                                   .build();

        syncClientWithUrlConnection= MediaStoreDataClient.builder()
                                                         .endpointOverride(uri)
                                                         .credentialsProvider(credentialsProvider)
                                                         .httpClient(UrlConnectionHttpClient.create())
                                                         .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureTransferEncodingHeaderInterceptor()))
                                                         .build();

        asyncClientWithNetty = MediaStoreDataAsyncClient.builder()
                                                        .endpointOverride(uri)
                                                        .credentialsProvider(getCredentialsProvider())
                                                        .httpClient(NettyNioAsyncHttpClient.create())
                                                        .overrideConfiguration(o -> o.addExecutionInterceptor(new CaptureTransferEncodingHeaderInterceptor()))
                                                        .build();

        putObjectRequest = PutObjectRequest.builder()
                                           .contentType("application/octet-stream")
                                           .path("/foo")
                                           .build();

        deleteObjectRequest = DeleteObjectRequest.builder()
                                                 .path("/foo")
                                                 .build();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        syncClientWithApache.deleteObject(deleteObjectRequest);
        Waiter.run(() -> syncClientWithApache.describeObject(r -> r.path("/foo")))
              .untilException(ObjectNotFoundException.class)
              .orFailAfter(Duration.ofMinutes(1));
        Thread.sleep(1000);
        mediaStoreClient.deleteContainer(r -> r.containerName(CONTAINER_NAME));
    }

    @Test
    public void apacheClientPutObject_withoutContentLength_sendsSuccessfully() {
        TestContentProvider provider = new TestContentProvider(RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
        syncClientWithApache.putObject(putObjectRequest, RequestBody.fromContentProvider(provider, "binary/octet-stream"));
        assertThat(CaptureTransferEncodingHeaderInterceptor.isChunked).isTrue();
    }

    @Test
    public void urlConnectionClientPutObject_withoutContentLength_sendsSuccessfully() {
        TestContentProvider provider = new TestContentProvider(RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
        syncClientWithUrlConnection.putObject(putObjectRequest, RequestBody.fromContentProvider(provider, "binary/octet-stream"));
        assertThat(CaptureTransferEncodingHeaderInterceptor.isChunked).isTrue();
    }

    @Test
    public void nettyClientPutObject_withoutContentLength_sendsSuccessfully() {
        asyncClientWithNetty.putObject(putObjectRequest, customAsyncRequestBodyWithoutContentLength("TestBody".getBytes())).join();
        assertThat(CaptureTransferEncodingHeaderInterceptor.isChunked).isTrue();
    }
}
