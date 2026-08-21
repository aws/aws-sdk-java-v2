package software.amazon.awssdk.mapper.dynamodb.test.resources.tables;

import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class TempTableWithBinaryKey {

    public static final String TEMP_BINARY_TABLE_NAME = "java-sdk-binary-" + System.currentTimeMillis();
    public static final String HASH_KEY_NAME = "hash";
    public static final Long READ_CAPACITY = 10L;
    public static final Long WRITE_CAPACITY = 5L;
    public static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
            ProvisionedThroughput.builder().readCapacityUnits(READ_CAPACITY).writeCapacityUnits(WRITE_CAPACITY).build();

    public static CreateTableRequest getCreateTableRequest() {
        return CreateTableRequest.builder()
                .tableName(TEMP_BINARY_TABLE_NAME)
                .keySchema(
                        KeySchemaElement.builder().attributeName(HASH_KEY_NAME)
                                .keyType(KeyType.HASH).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(HASH_KEY_NAME)
                                .attributeType(ScalarAttributeType.B).build())
                .provisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT)
                .build();
    }

}
