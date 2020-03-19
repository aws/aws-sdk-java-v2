/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.mocktests;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.stubResponseWithUnprocessedKeys;
import static software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.stubSuccessfulResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class AsyncBatchGetItemTest {

    private DynamoDbEnhancedAsyncClient enhancedClient;
    private DynamoDbAsyncTable<Record> table;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Before
    public void setup() {

        DynamoDbAsyncClient dynamoDbClient =
            DynamoDbAsyncClient.builder()
                               .region(Region.US_WEST_2)
                               .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                               .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                               .build();
        enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                                    .dynamoDbClient(dynamoDbClient)
                                                    .build();
        StaticTableSchema<Record> tableSchema = StaticTableSchema.builder(Record.class)
                                                                 .newItemSupplier(Record::new)
                                                                 .addAttribute(Integer.class,
                                                                               a -> a.name("id")
                                                                                     .getter(Record::getId)
                                                                                     .setter(Record::setId)
                                                                                     .tags(primaryPartitionKey()))
                                                                 .build();
        table = enhancedClient.table("table", tableSchema);
    }

    @Test
    public void successfulResponseWithoutUnprocessedKeys_NoNextPage() throws InterruptedException {
        stubSuccessfulResponse();
        SdkPublisher<BatchGetResultPage> batchGetResultPages = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .build()));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        VerifyPageSubscriber verifyPageSubscriber = new VerifyPageSubscriber(countDownLatch);
        batchGetResultPages.subscribe(verifyPageSubscriber);
        countDownLatch.await(1000, TimeUnit.SECONDS);
        assertThat(verifyPageSubscriber.pages.size()).isEqualTo(1);
        assertThat(verifyPageSubscriber.pages.get(0).getResultsForTable(table).size()).isEqualTo(3);
    }

    @Test
    public void responseWithUnprocessedKeys_iteratePage_shouldFetchUnprocessedKeys() throws InterruptedException {
        stubResponseWithUnprocessedKeys();
        SdkPublisher<BatchGetResultPage> batchGetResultPages = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .build()));
        CountDownLatch countDownLatch = new CountDownLatch(1);
        VerifyPageSubscriber verifyPageSubscriber = new VerifyPageSubscriber(countDownLatch);
        batchGetResultPages.subscribe(verifyPageSubscriber);
        countDownLatch.await(1000, TimeUnit.SECONDS);
        assertThat(verifyPageSubscriber.pages.size()).isEqualTo(2);
        assertThat(verifyPageSubscriber.pages.get(0).getResultsForTable(table).size()).isEqualTo(2);
        assertThat(verifyPageSubscriber.pages.get(1).getResultsForTable(table).size()).isEqualTo(1);
    }

    private static final class VerifyPageSubscriber implements Subscriber<BatchGetResultPage> {
        private Subscription subscription;
        private List<BatchGetResultPage> pages = new ArrayList<>();
        private CountDownLatch countDownLatch;

        VerifyPageSubscriber(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            subscription.request(1);
        }

        @Override
        public void onNext(BatchGetResultPage batchGetResultPage) {
            pages.add(batchGetResultPage);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            countDownLatch.countDown();
        }

        @Override
        public void onComplete() {
            countDownLatch.countDown();
        }
    }
}
