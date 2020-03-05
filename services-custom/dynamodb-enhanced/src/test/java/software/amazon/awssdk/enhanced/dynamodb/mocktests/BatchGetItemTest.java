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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.Attributes.attribute;
import static software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.stubResponseWithUnprocessedKeys;
import static software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.stubSuccessfulResponse;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TypeToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mocktests.BatchGetTestUtils.Record;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class BatchGetItemTest {

    private DynamoDbClient dynamoDbClient;
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<Record> table;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Before
    public void setup() {

        dynamoDbClient = DynamoDbClient.builder()
                                       .region(Region.US_WEST_2)
                                       .credentialsProvider(() -> AwsBasicCredentials.create("foo", "bar"))
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(dynamoDbClient)
                                               .build();
        StaticTableSchema<Record> tableSchema = StaticTableSchema.builder(Record.class)
                                                                 .newItemSupplier(Record::new)
                                                                 .attributes(attribute("id", TypeToken.of(Integer.class),
                                                                                       Record::getId,
                                                                                       Record::setId).as(primaryPartitionKey()))
                                                                 .build();
        table = enhancedClient.table("table", tableSchema);
    }

    @Test
    public void successfulResponseWithoutUnprocessedKeys_NoNextPage() {
        stubSuccessfulResponse();
        SdkIterable<BatchGetResultPage> batchGetResultPages = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .addGetItem(i -> i.key(k -> k.partitionValue(0)))
                     .build()));

        assertThat(batchGetResultPages.stream().count()).isEqualTo(1);
    }

    @Test
    public void responseWithUnprocessedKeys_iteratePage_shouldFetchUnprocessedKeys() {
        stubResponseWithUnprocessedKeys();
        SdkIterable<BatchGetResultPage> batchGetResultPages = enhancedClient.batchGetItem(r -> r.readBatches(
            ReadBatch.builder(Record.class)
                     .mappedTableResource(table)
                     .addGetItem(i -> i.key(k -> k.partitionValue("1")))
                     .build()));

        Iterator<BatchGetResultPage> iterator = batchGetResultPages.iterator();
        BatchGetResultPage firstPage = iterator.next();
        List<Record> resultsForTable = firstPage.getResultsForTable(table);
        assertThat(resultsForTable.size()).isEqualTo(2);
        BatchGetResultPage secondPage = iterator.next();
        assertThat(secondPage.getResultsForTable(table).size()).isEqualTo(1);
        assertThat(iterator).isEmpty();
    }
}
