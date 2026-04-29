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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTimeToLiveEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateTimeToLiveEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveStatus;

@RunWith(Parameterized.class)
public class SyncTimeToLiveTableOperationSchemaVariantsTest extends LocalDynamoDbSyncTestBase {
    private static final String TABLE_NAME = "table-name";

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private final TableSchema<?> ttlSchema;
    private final TableSchema<?> schemaWithoutTtl;
    private final String schemaType;

    private DynamoDbTable<?> mappedTable;

    public SyncTimeToLiveTableOperationSchemaVariantsTest(String schemaType,
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
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName(TABLE_NAME))
                                                          .build());
    }

    @Test
    public void describeTimeToLive_returnsDisabledStatus_whenTableIsNew() {
        DescribeTimeToLiveEnhancedResponse response = mappedTable.describeTimeToLive();

        assertThat(response.timeToLiveDescription().timeToLiveStatus())
            .as(schemaType)
            .isEqualTo(TimeToLiveStatus.DISABLED);
    }

    @Test
    public void describeTimeToLive_returnsDisabledStatus_whenSchemaHasNoTtlMetadata() {
        DescribeTimeToLiveEnhancedResponse response = enhancedClient.table(getConcreteTableName(TABLE_NAME), schemaWithoutTtl)
                                                                    .describeTimeToLive();

        assertThat(response.timeToLiveDescription().timeToLiveStatus())
            .as(schemaType)
            .isEqualTo(TimeToLiveStatus.DISABLED);
    }

    @Test
    public void describeTimeToLive_returnsExpirationAttribute_whenTtlWasEnabled() {
        mappedTable.updateTimeToLive(true);

        DescribeTimeToLiveEnhancedResponse response = mappedTable.describeTimeToLive();

        assertThat(response.timeToLiveDescription().attributeName())
            .as(schemaType)
            .isEqualTo("expirationDate");
    }

    @Test
    public void updateTimeToLive_returnsEnabledSpecification_whenEnablingTtl() {
        UpdateTimeToLiveEnhancedResponse response = mappedTable.updateTimeToLive(true);

        assertThat(response.timeToLiveSpecification().enabled()).as(schemaType).isTrue();
        assertThat(response.timeToLiveSpecification().attributeName()).as(schemaType).isEqualTo("expirationDate");
    }

    @Test
    public void updateTimeToLive_returnsDisabledSpecification_whenDisablingTtl() {
        UpdateTimeToLiveEnhancedResponse enableResponse = mappedTable.updateTimeToLive(true);
        assertThat(enableResponse.timeToLiveSpecification().enabled()).as(schemaType).isTrue();

        UpdateTimeToLiveEnhancedResponse response = mappedTable.updateTimeToLive(false);

        assertThat(response.timeToLiveSpecification().enabled()).as(schemaType).isFalse();
        assertThat(response.timeToLiveSpecification().attributeName()).as(schemaType).isEqualTo("expirationDate");
    }

    @Test
    public void updateTimeToLive_throwsException_whenTtlIsAlreadyEnabled() {
        mappedTable.updateTimeToLive(true);

        assertThatThrownBy(() -> mappedTable.updateTimeToLive(true))
            .as(schemaType)
            .hasMessageContaining("TimeToLive is already enabled");
    }

    @Test
    public void updateTimeToLive_throwsException_whenTtlIsAlreadyDisabled() {
        assertThatThrownBy(() -> mappedTable.updateTimeToLive(false))
            .as(schemaType)
            .hasMessageContaining("TimeToLive is already disabled");
    }

    @Test
    public void updateTimeToLive_throwsException_whenSchemaHasNoTtlMetadata() {
        assertThatThrownBy(() -> enhancedClient.table(getConcreteTableName(TABLE_NAME), schemaWithoutTtl)
                                               .updateTimeToLive(true))
            .as(schemaType)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Custom TTL metadata object is null");
    }
}
