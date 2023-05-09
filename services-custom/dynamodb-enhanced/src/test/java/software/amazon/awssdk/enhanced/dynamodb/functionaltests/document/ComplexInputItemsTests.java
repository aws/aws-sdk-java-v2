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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class ComplexInputItemsTests extends LocalDynamoDbSyncTestBase {
    private final String tableName = getConcreteTableName("table-name");
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbClient lowLevelClient;
    private DynamoDbTable<EnhancedDocument> docMappedtable;

    @Before
    public void setUp() {
        lowLevelClient = getDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(lowLevelClient)
                                               .build();
        docMappedtable = enhancedClient.table(tableName,
                                              TableSchema.documentSchemaBuilder()
                                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                               "id",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(TableMetadata.primaryIndexName(), "sort",
                                                                          AttributeValueType.N)
                                                         .attributeConverterProviders(defaultProvider())
                                                         .build());
        docMappedtable.createTable();
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(tableName)
                                                          .build());
    }

    @Test
    public void getAndPutDocumentWithNoAttributeConverters() {

        docMappedtable.putItem(EnhancedDocument.builder()
                                               .putString("id", "one")
                                               .putNumber("sort", 1)
                                               .putString("element", "noAttributeConverter")
                                               .build());

        EnhancedDocument noConverterInGetItem = docMappedtable.getItem(EnhancedDocument.builder()
                                                                                       .putString("id", "one")
                                                                                       .putNumber("sort", 1)
                                                                                       .build());
        assertThat(noConverterInGetItem.toJson(), is("{\"id\":\"one\",\"sort\":1,\"element\":\"noAttributeConverter\"}"));

    }


    @Test
    public void bytesInAlTypes() {

        Map<String, SdkBytes> bytesMap = new LinkedHashMap<>();
        bytesMap.put("key1", SdkBytes.fromByteArray("1".getBytes(StandardCharsets.UTF_8)));
        bytesMap.put("key2", SdkBytes.fromByteArray("2".getBytes(StandardCharsets.UTF_8)));
        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                            .putString("id", "allTypesBytes")
                                                            .putNumber("sort", 2)
                                                            .put("put", SdkBytes.fromUtf8String("1"), SdkBytes.class)
                                                            .putBytes("putBytes",
                                                                      SdkBytes.fromByteBuffer(ByteBuffer.wrap("1".getBytes(StandardCharsets.UTF_8))))
                                                            .putBytesSet("putBytesSet",
                                                                         Stream.of(SdkBytes.fromUtf8String("1"),
                                                                                   SdkBytes.fromUtf8String("2")).collect(Collectors.toCollection(LinkedHashSet::new)))
                                                            .putList("putBytesList",
                                                                     Stream.of(SdkBytes.fromUtf8String("1"),
                                                                               SdkBytes.fromUtf8String("2")).collect(Collectors.toList()),
                                                                     EnhancedType.of(SdkBytes.class))
                                                            .putMap("bytesMap", bytesMap, EnhancedType.of(String.class),
                                                                    EnhancedType.of(SdkBytes.class))
                                                            .build();

        docMappedtable.putItem(enhancedDocument);


        EnhancedDocument retrievedItem = docMappedtable.getItem(EnhancedDocument.builder()
                                                                                .putString("id", "allTypesBytes")
                                                                                .putNumber("sort", 2)
                                                                                .build());

        assertThat(retrievedItem.toJson(), is("{\"putBytesSet\":[\"MQ==\",\"Mg==\"],\"putBytes\":\"MQ==\",\"putBytesList\":[\"MQ==\",\"Mg==\"],\"id\":\"allTypesBytes\",\"sort\":2,\"bytesMap\":{\"key1\":\"MQ==\",\"key2\":\"Mg==\"},\"put\":\"MQ==\"}"
        ));
    }
}
