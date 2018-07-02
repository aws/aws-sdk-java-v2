/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;
import software.amazon.awssdk.testutils.RandomTempFile;

public class ListObjectsV2PaginatorsIntegrationTest extends S3IntegrationTestBase {

    private static final long OBJECT_COUNT = 15;

    /**
     * Content length for sample keys created by these tests.
     */
    private static final long CONTENT_LENGTH = 123;

    /**
     * The name of the bucket created, used, and deleted by these tests.
     */
    private static String bucketName = temporaryBucketName("list-objects-v2-integ-test");

    private static String emptyBucketName = temporaryBucketName("list-objects-integ-test-emptybucket");

    /**
     * List of all keys created  by these tests.
     */
    private static List<String> keys = new ArrayList<>();

    private static final ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder().maxKeys(3);

    /**
     * Releases all resources created in this test.
     */
    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(bucketName);
        deleteBucketAndAllContents(emptyBucketName);
    }

    /**
     * Creates all the test resources for the tests.
     */
    @BeforeClass
    public static void createResources() throws Exception {
        createBucket(bucketName);
        createBucket(emptyBucketName);

        NumberFormat numberFormatter = new DecimalFormat("##00");
        for (int i = 1; i <= OBJECT_COUNT; i++) {
            createKey("key-" + numberFormatter.format(i));
        }
    }

    /**
     * Creates a test object in S3 with the specified name, using random ASCII
     * data of the default content length as defined in this test class.
     *
     * @param key The key under which to create the object in this test class'
     *            test bucket.
     */
    private static void createKey(String key) throws Exception {

        File file = new RandomTempFile("list-objects-integ-test-" + new Date().getTime(), CONTENT_LENGTH);

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(bucketName)
                                     .key(key)
                                     .build(),
                     RequestBody.fromFile(file));
        keys.add(key);
    }

    @Test
    public void test_SyncResponse_onEmptyBucket() {
        ListObjectsV2Iterable iterable = s3.listObjectsV2Paginator(requestBuilder.bucket(emptyBucketName).build());

        assertThat(iterable.contents().stream().count(), equalTo(0L));

        assertThat(iterable.stream()
                           .flatMap(r -> r.contents() != null ? r.contents().stream() : Stream.empty())
                           .count(),
                   equalTo(0L));
    }

    @Test
    public void test_SyncResponse_onNonEmptyBucket() {
        ListObjectsV2Iterable iterable = s3.listObjectsV2Paginator(requestBuilder.bucket(bucketName).build());

        assertThat(iterable.contents().stream().count(), equalTo(OBJECT_COUNT));

        assertThat(iterable.stream().flatMap(r -> r.contents().stream()).count(), equalTo(OBJECT_COUNT));
    }

    @Test
    public void test_AsyncResponse_OnNonEmptyBucket() throws ExecutionException, InterruptedException {
        ListObjectsV2Publisher publisher = s3Async.listObjectsV2Paginator(requestBuilder.bucket(bucketName).build());

        publisher.subscribe(new Subscriber<ListObjectsV2Response>() {
            private Subscription subscription;
            private int keyCount;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                keyCount = 0;
                subscription.request(2);
            }

            @Override
            public void onNext(ListObjectsV2Response response) {
                keyCount += response.keyCount();
                subscription.request(1);
                subscription.request(2);
            }

            @Override
            public void onError(Throwable t) {
                fail("Error receiving response" + t.getMessage());
            }

            @Override
            public void onComplete() {
                assertThat(keyCount, equalTo(OBJECT_COUNT));
            }
        });
        Thread.sleep(3_000);


        // count objects using forEach
        final long[] count = {0};
        CompletableFuture<Void> future = publisher.subscribe(response -> {
            count[0] += response.keyCount();
        });
        future.get();
        assertThat(count[0], equalTo(OBJECT_COUNT));


        // Use ForEach: collect objects into a list
        List<S3Object> objects = new ArrayList<>();
        CompletableFuture<Void> future2 = publisher.subscribe(response -> {
            objects.addAll(response.contents());
        });
        future2.get();
        assertThat(Long.valueOf(objects.size()), equalTo(OBJECT_COUNT));
    }

    @Test
    public void test_AsyncResponse_OnEmptyBucket() throws ExecutionException, InterruptedException {
        ListObjectsV2Publisher publisher = s3Async.listObjectsV2Paginator(requestBuilder.bucket(emptyBucketName).build());

        final int[] count = {0};
        CompletableFuture<Void> future = publisher.subscribe(response -> {
            count[0] += response.keyCount();
        });
        future.get();
        assertThat(count[0], equalTo(0));
    }

    @Test
    public void test_AsyncResponse_UsingRxJava() {
        ListObjectsV2Publisher publisher = s3Async.listObjectsV2Paginator(requestBuilder.bucket(bucketName).build());

        Single<List<S3Object>> objects = Flowable.fromPublisher(publisher)
                                                 .flatMapIterable(ListObjectsV2Response::contents)
                                                 .toList();

        // There are multiple fluent methods to convert Single type to a different form
        List<S3Object> objectList = objects.blockingGet();
        assertThat(Long.valueOf(objectList.size()), equalTo(OBJECT_COUNT));
    }

    private class TestSubscriber implements Subscriber<ListObjectsV2Response> {
        private Subscription subscription;
        private ListObjectsV2Response lastPage;
        private final long requestCount;
        private long keyCount;
        private long requestsCompleted;
        private boolean isDone;

        public TestSubscriber(long requestCount) {
            this.requestCount = requestCount;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            subscription.request(requestCount);
        }

        @Override
        public void onNext(ListObjectsV2Response response) {
            lastPage = response;
            keyCount += response.keyCount();
            requestsCompleted++;
        }

        @Override
        public void onError(Throwable t) {
            fail("Error receiving response" + t.getMessage());
        }

        @Override
        public void onComplete() {
            isDone = true;
        }

        public long getKeyCount() {
            return keyCount;
        }

        public ListObjectsV2Response getLastPage() {
            return lastPage;
        }

        public boolean isDone() {
            return isDone || requestCount == requestsCompleted;
        }
    }
}
