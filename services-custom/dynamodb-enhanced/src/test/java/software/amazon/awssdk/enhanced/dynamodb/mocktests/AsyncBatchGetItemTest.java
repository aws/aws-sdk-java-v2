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
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase.drainPublisher;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.stubResponseWithUnprocessedKeys;
import static software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.stubSuccessfulResponse;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher;
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
                               .endpointDiscoveryEnabled(false)
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
    public void successfulResponseWithoutUnprocessedKeys_NoNextPage() {
        stubSuccessfulResponse();
        SdkPublisher<BatchGetResultPage> publisher = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .build()));

        List<BatchGetResultPage> batchGetResultPages = drainPublisher(publisher, 1);

        assertThat(batchGetResultPages.size()).isEqualTo(1);
        assertThat(batchGetResultPages.get(0).resultsForTable(table).size()).isEqualTo(3);
    }

    @Test
    public void successfulResponseWithoutUnprocessedKeys_viaFlattenedItems_NoNextPage() {
        stubSuccessfulResponse();
        BatchGetResultPagePublisher publisher = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .build()));

        List<Record> records = drainPublisher(publisher.resultsForTable(table), 3);
        assertThat(records.size()).isEqualTo(3);
    }

    @Test
    public void responseWithUnprocessedKeys_iteratePage_shouldFetchUnprocessedKeys() throws InterruptedException {
        stubResponseWithUnprocessedKeys();
        SdkPublisher<BatchGetResultPage> publisher = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .build()));

        List<BatchGetResultPage> batchGetResultPages = drainPublisher(publisher, 2);
        assertThat(batchGetResultPages.size()).isEqualTo(2);
        assertThat(batchGetResultPages.get(0).resultsForTable(table).size()).isEqualTo(2);
        assertThat(batchGetResultPages.get(1).resultsForTable(table).size()).isEqualTo(1);
        assertThat(batchGetResultPages.size()).isEqualTo(2);
    }

    @Test
    public void responseWithUnprocessedKeys_iterateItems_shouldFetchUnprocessedKeys() throws InterruptedException {
        stubResponseWithUnprocessedKeys();
        BatchGetResultPagePublisher publisher = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .build()));

        List<Record> records = drainPublisher(publisher.resultsForTable(table), 3);
        assertThat(records.size()).isEqualTo(3);
    }
}
