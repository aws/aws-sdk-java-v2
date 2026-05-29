package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordForUpdateExpressions;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AsyncUpdateExpressionTest extends LocalDynamoDbAsyncTestBase {
    private static final TableSchema<RecordForUpdateExpressions> TABLE_SCHEMA =
        TableSchema.fromClass(RecordForUpdateExpressions.class);

    private final DynamoDbEnhancedAsyncClient enhancedAsyncClient =
        DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();

    private final DynamoDbAsyncTable<RecordForUpdateExpressions> mappedTable =
        enhancedAsyncClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(r -> r.tableName(getConcreteTableName("table-name"))).join();
    }

    @Test
    public void updateItem_withConditionExpression_shouldNotApplyUpdateWhenConditionMatches() {
        RecordForUpdateExpressions seed = new RecordForUpdateExpressions();
        seed.setId("id1");
        seed.setStringAttribute("init");
        mappedTable.putItem(seed).join();

        RecordForUpdateExpressions update = new RecordForUpdateExpressions();
        update.setId("id1");
        update.setStringAttribute("changed");
        mappedTable.updateItem(r -> r.item(update)
                                   .conditionExpression(Expression.builder()
                                                                 .expression("id = :id")
                                                                 .expressionValues(Collections.singletonMap(":id",
                                                                     AttributeValue.builder().s("id1").build()))
                                                                 .build()))
                   .join();

        RecordForUpdateExpressions persisted = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id1"))).join();
        assertThat(persisted.getStringAttribute()).isEqualTo("init");
    }
}
