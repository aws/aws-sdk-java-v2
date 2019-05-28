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

package software.amazon.awssdk.services.mediastoredata;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.mediastore.MediaStoreClient;
import software.amazon.awssdk.services.mediastore.model.Container;
import software.amazon.awssdk.services.mediastore.model.ContainerStatus;
import software.amazon.awssdk.services.mediastore.model.DescribeContainerResponse;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectRequest;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class MediaStoreDataIntegrationTest extends AwsIntegrationTestBase {
    private static final String CONTAINER_NAME = "java-sdk-test-" + Instant.now().toEpochMilli();
    private static final String PATH = "/foo/bar";

    private static MediaStoreClient mediaStoreClient;
    private static MediaStoreDataClient syncClientWithApache;
    private static MediaStoreDataClient syncClientWithUrlConnection;
    private static MediaStoreDataAsyncClient asyncClient;

    private static Container container;
    private static PutObjectRequest putObjectRequest;

    @BeforeClass
    public static void setup() {
        mediaStoreClient = MediaStoreClient.builder()
                                           .credentialsProvider(getCredentialsProvider())
                                           .httpClient(ApacheHttpClient.builder().build())
                                           .build();
        container = createContainer();
        URI uri = URI.create(container.endpoint());

        syncClientWithApache = MediaStoreDataClient.builder()
                                                   .endpointOverride(uri)
                                                   .credentialsProvider(getCredentialsProvider())
                                                   .httpClient(ApacheHttpClient.builder().build())
                                                   .build();

        syncClientWithUrlConnection= MediaStoreDataClient.builder()
                                                         .endpointOverride(uri)
                                                         .credentialsProvider(getCredentialsProvider())
                                                         .httpClient(UrlConnectionHttpClient.create())
                                                         .build();

        asyncClient = MediaStoreDataAsyncClient.builder()
                                               .endpointOverride(uri)
                                               .credentialsProvider(getCredentialsProvider())
                                               .build();

        putObjectRequest = PutObjectRequest.builder()
                                           .contentType("application/octet-stream")
                                           .path(PATH)
                                           .build();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        waitContainerToBeActive();

        syncClientWithApache.deleteObject(b -> b.path(PATH));

        mediaStoreClient.deleteContainer(r -> r.containerName(CONTAINER_NAME));
    }

    @org.junit.Test
    public void putObject_sync_apacheHttpClient() {
        syncClientWithApache.putObject(putObjectRequest, RequestBody.fromString("foobar"));
    }

    @org.junit.Test
    public void putObject_sync_urlConnectionHttpClient() {
        syncClientWithUrlConnection.putObject(putObjectRequest,
                                              RequestBody.fromInputStream(
                                                  new ByteArrayInputStream("foobar".getBytes()), 6));
    }

    @org.junit.Test
    public void putObject_asyncClient_withContentLength() {
        asyncClient.putObject(putObjectRequest, AsyncRequestBody.fromString("foobar")).join();
    }

    @org.junit.Test
    public void putObject_asyncClient_withoutContentLength() {
        asyncClient.putObject(putObjectRequest, customAsyncRequestBody()).join();
    }

    private static Container createContainer() {
        mediaStoreClient.createContainer(r -> r.containerName(CONTAINER_NAME));

        DescribeContainerResponse response = waitContainerToBeActive();
        return response.container();
    }

    private static DescribeContainerResponse waitContainerToBeActive() {
        return Waiter.run(() -> mediaStoreClient.describeContainer(r -> r.containerName(CONTAINER_NAME)))
                     .until(r -> ContainerStatus.ACTIVE.equals(r.container().status()))
                     .orFailAfter(Duration.ofMinutes(3));
    }

    private AsyncRequestBody customAsyncRequestBody() {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(new ByteArrayAsyncRequestBody("Random text".getBytes()))
                        .subscribe(s);
            }
        };
    }
}
