package software.amazon.awssdk.mapper.dynamodb.test.resources.tables;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class TempTableWithBinaryKey {

    public static final String TEMP_BINARY_TABLE_NAME = "java-sdk-binary-" + System.currentTimeMillis();
    public static final String HASH_KEY_NAME = "hash";
    public static final Long READ_CAPACITY = 10L;
    public static final Long WRITE_CAPACITY = 5L;
    public static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
            new ProvisionedThroughput().withReadCapacityUnits(READ_CAPACITY).withWriteCapacityUnits(WRITE_CAPACITY);

    public static CreateTableRequest getCreateTableRequest() {
        CreateTableRequest request = new CreateTableRequest()
                .withTableName(TEMP_BINARY_TABLE_NAME)
                .withKeySchema(
                        new KeySchemaElement().withAttributeName(HASH_KEY_NAME)
                                .withKeyType(KeyType.HASH))
                .withAttributeDefinitions(
                        new AttributeDefinition().withAttributeName(
                                HASH_KEY_NAME).withAttributeType(
                                ScalarAttributeType.B));
        request.setProvisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT);
        return request;
    }

}
