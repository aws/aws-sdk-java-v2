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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTimeToLiveEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateTimeToLiveEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveStatus;

@RunWith(Parameterized.class)
public class AsyncTimeToLiveTableOperationSchemaVariantsTest extends LocalDynamoDbAsyncTestBase {
    private static final String TABLE_NAME = "table-name";

    private final DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                                                                           .dynamoDbClient(getDynamoDbAsyncClient())
                                                                                           .build();

    private final TableSchema<?> ttlSchema;
    private final TableSchema<?> schemaWithoutTtl;
    private final String schemaType;

    private DynamoDbAsyncTable<?> mappedTable;

    public AsyncTimeToLiveTableOperationSchemaVariantsTest(String schemaType,
                                                           TableSchema<?> ttlSchema,
                                                           TableSchema<?> schemaWithoutTtl) {
        this.ttlSchema = ttlSchema;
        this.schemaWithoutTtl = schemaWithoutTtl;
        this.schemaType = schemaType;
    }

    @Parameters(name = "{index}; {0}")
    public static Collection<Object[]> parameters() {
        return TimeToLiveSchemaVariants.data();
    }

    @Before
    public void createTable() {
        mappedTable = enhancedClient.table(getConcreteTableName(TABLE_NAME), ttlSchema);
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                               .tableName(getConcreteTableName(TABLE_NAME))
                                                               .build()).join();
    }

    @Test
    public void describeTimeToLive_returnsDisabledStatus_whenTableIsNew() {
        DescribeTimeToLiveEnhancedResponse response = mappedTable.describeTimeToLive().join();

        assertThat(response.timeToLiveDescription().timeToLiveStatus())
            .as(schemaType)
            .isEqualTo(TimeToLiveStatus.DISABLED);
    }

    @Test
    public void describeTimeToLive_returnsDisabledStatus_whenSchemaHasNoTtlMetadata() {
        DescribeTimeToLiveEnhancedResponse response = enhancedClient.table(getConcreteTableName(TABLE_NAME), schemaWithoutTtl)
                                                                    .describeTimeToLive().join();

        assertThat(response.timeToLiveDescription().timeToLiveStatus())
            .as(schemaType)
            .isEqualTo(TimeToLiveStatus.DISABLED);
    }

    @Test
    public void describeTimeToLive_returnsExpirationAttribute_whenTtlWasEnabled() {
        mappedTable.updateTimeToLive(true).join();

        DescribeTimeToLiveEnhancedResponse response = mappedTable.describeTimeToLive().join();

        assertThat(response.timeToLiveDescription().attributeName())
            .as(schemaType)
            .isEqualTo("expirationDate");
    }

    @Test
    public void updateTimeToLive_returnsEnabledSpecification_whenEnablingTtl() {
        UpdateTimeToLiveEnhancedResponse response = mappedTable.updateTimeToLive(true).join();

        assertThat(response.table().enabled()).as(schemaType).isTrue();
        assertThat(response.table().attributeName()).as(schemaType).isEqualTo("expirationDate");
    }

    @Test
    public void updateTimeToLive_returnsDisabledSpecification_whenDisablingTtl() {
        UpdateTimeToLiveEnhancedResponse enableResponse = mappedTable.updateTimeToLive(true).join();
        assertThat(enableResponse.table().enabled()).as(schemaType).isTrue();

        UpdateTimeToLiveEnhancedResponse response = mappedTable.updateTimeToLive(false).join();

        assertThat(response.table().enabled()).as(schemaType).isFalse();
        assertThat(response.table().attributeName()).as(schemaType).isEqualTo("expirationDate");
    }

    @Test
    public void updateTimeToLive_throwsException_whenTtlIsAlreadyEnabled() {
        mappedTable.updateTimeToLive(true).join();

        assertThatThrownBy(() -> mappedTable.updateTimeToLive(true).join())
            .as(schemaType)
            .hasMessageContaining("TimeToLive is already enabled");
    }

    @Test
    public void updateTimeToLive_throwsException_whenTtlIsAlreadyDisabled() {
        assertThatThrownBy(() -> mappedTable.updateTimeToLive(false).join())
            .as(schemaType)
            .hasMessageContaining("TimeToLive is already disabled");
    }

    @Test
    public void updateTimeToLive_throwsException_whenSchemaHasNoTtlMetadata() {
        assertThatThrownBy(() -> enhancedClient.table(getConcreteTableName(TABLE_NAME), schemaWithoutTtl)
                                               .updateTimeToLive(true).join())
            .as(schemaType)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Custom TTL metadata object is null");
    }
}
