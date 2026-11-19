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
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.staticImmutableRecordSchema;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.staticRecordSchema;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithArrayList;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithHashMap;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithHashSet;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithObject;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.BeanWithUnsupported;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ConverterImmutable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.ConverterRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.DefaultProviderBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.SingleGsiSortBean;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.DefaultAttributeConverterProviderTestModels.UnsupportedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexDescription;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

/**
 * Enhanced Client table bind and createTable for DefaultAttributeConverterProvider schemas.
 * <p>
 * Each case runs for {@link DynamoDbEnhancedClientType#SYNC} and {@link DynamoDbEnhancedClientType#ASYNC}.
 * The class covers successful bind and create for all five schema kinds, bind failures for
 * unconvertible attributes, omitted-provider bind, and GSI metadata on create.
 */
public class DefaultAttributeConverterProviderTableSetupTest extends LocalDynamoDbTestBase {

    @BeforeAll
    public static void startLocalDynamoDb() {
        localDynamoDb().start();
    }

    @AfterAll
    public static void stopLocalDynamoDbForJunit5() {
        localDynamoDb().stop();
    }

    @BeforeEach
    public void clearBeanSchemaCache() throws Exception {
        Method clear = BeanTableSchema.class.getDeclaredMethod("clearSchemaCache");
        clear.setAccessible(true);
        clear.invoke(null);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a table from a convertible bean schema")
    public void tableThenCreateTable_whenBeanSchemaIsConvertible_createsTable(DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "bean");
        try {
            client.createTable(TableSchema.fromBean(ConverterRecord.class));

            assertThat(client.tableExists()).isTrue();
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a table from a convertible immutable schema")
    public void tableThenCreateTable_whenImmutableSchemaIsConvertible_createsTable(DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "immutable");
        try {
            client.createTable(TableSchema.fromImmutableClass(ConverterImmutable.class));

            assertThat(client.tableExists()).isTrue();
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a table from a convertible static schema")
    public void tableThenCreateTable_whenStaticSchemaIsConvertible_createsTable(DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "static");
        try {
            client.createTable(staticRecordSchema());

            assertThat(client.tableExists()).isTrue();
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a table from a convertible static immutable schema")
    public void tableThenCreateTable_whenStaticImmutableSchemaIsConvertible_createsTable(DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "static-imm");
        try {
            client.createTable(staticImmutableRecordSchema());

            assertThat(client.tableExists()).isTrue();
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a table from a convertible document schema")
    public void tableThenCreateTable_whenDocumentSchemaIsConvertible_createsTable(DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "document");
        try {
            client.createTable(documentSchema());

            assertThat(client.tableExists()).isTrue();
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a table when the bean omits converter providers")
    public void tableThenCreateTable_whenConverterProvidersAreOmitted_createsTable(DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "omitted");
        try {
            client.createTable(TableSchema.fromBean(DefaultProviderBean.class));

            assertThat(client.tableExists()).isTrue();
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Creates a GSI with an unspecified integer sort key")
    public void tableThenCreateTable_whenBeanDeclaresGsi_createsIndexWithUnspecifiedIntegerSort(
        DynamoDbEnhancedClientType clientType) {
        SetupClient client = openClient(clientType, "gsi");
        try {
            TableSchema<SingleGsiSortBean> schema = TableSchema.fromBean(SingleGsiSortBean.class);

            client.createTableWithGsi(schema, "gsi");

            assertThat(schema.tableMetadata().indexPartitionKey("gsi")).isEqualTo("gsiKey");
            assertThat(schema.tableMetadata().indexSortKey("gsi")).contains("gsiSort");
            assertThat(client.globalSecondaryIndexNames()).contains("gsi");
        } finally {
            client.deleteTable();
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Rejects table setup when a bean Object attribute has no converter")
    public void table_whenBeanDeclaresObject_throwsConverterNotFoundBeforeSetupCompletes(
        DynamoDbEnhancedClientType clientType) {
        assertThatThrownBy(() -> table(clientType, "unused-table", TableSchema.fromBean(BeanWithObject.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(Object.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Rejects table setup when a bean attribute has no converter")
    public void table_whenBeanDeclaresUnsupportedType_throwsIllegalStateException(DynamoDbEnhancedClientType clientType) {
        assertThatThrownBy(() -> table(clientType, "unused-table", TableSchema.fromBean(BeanWithUnsupported.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + EnhancedType.of(UnsupportedType.class));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Rejects table setup when a bean attribute is ArrayList")
    public void table_whenBeanDeclaresArrayList_throwsIllegalStateException(DynamoDbEnhancedClientType clientType) {
        EnhancedType<ArrayList<String>> type = new EnhancedType<ArrayList<String>>() { };
        assertThatThrownBy(() -> table(clientType, "unused-table", TableSchema.fromBean(BeanWithArrayList.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Rejects table setup when a bean attribute is HashSet")
    public void table_whenBeanDeclaresHashSet_throwsIllegalStateException(DynamoDbEnhancedClientType clientType) {
        EnhancedType<HashSet<String>> type = new EnhancedType<HashSet<String>>() { };
        assertThatThrownBy(() -> table(clientType, "unused-table", TableSchema.fromBean(BeanWithHashSet.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DynamoDbEnhancedClientType.class)
    @DisplayName("Rejects table setup when a bean attribute is HashMap")
    public void table_whenBeanDeclaresHashMap_throwsIllegalStateException(DynamoDbEnhancedClientType clientType) {
        EnhancedType<HashMap<String, Integer>> type = new EnhancedType<HashMap<String, Integer>>() { };
        assertThatThrownBy(() -> table(clientType, "unused-table", TableSchema.fromBean(BeanWithHashMap.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Converter not found for " + type);
    }

    private SetupClient openClient(DynamoDbEnhancedClientType clientType, String tableNameSuffix) {
        return clientType == DynamoDbEnhancedClientType.SYNC
               ? new SyncSetupClient(tableNameSuffix)
               : new AsyncSetupClient(tableNameSuffix);
    }

    private void table(DynamoDbEnhancedClientType clientType, String tableName, TableSchema<?> schema) {
        if (clientType == DynamoDbEnhancedClientType.SYNC) {
            DynamoDbEnhancedClient.builder()
                                  .dynamoDbClient(localDynamoDb().createClient())
                                  .build()
                                  .table(tableName, schema);
            return;
        }
        DynamoDbEnhancedAsyncClient.builder()
                                   .dynamoDbClient(localDynamoDb().createAsyncClient())
                                   .build()
                                   .table(tableName, schema);
    }

    private static TableSchema<EnhancedDocument> documentSchema() {
        return TableSchema.documentSchemaBuilder()
                          .addIndexPartitionKey(TableMetadata.primaryIndexName(), "id", AttributeValueType.S)
                          .build();
    }

    private interface SetupClient {
        <T> void createTable(TableSchema<T> schema);

        <T> void createTableWithGsi(TableSchema<T> schema, String indexName);

        boolean tableExists();

        List<String> globalSecondaryIndexNames();

        void deleteTable();
    }

    private final class SyncSetupClient implements SetupClient {
        private final DynamoDbClient dynamoDbClient = localDynamoDb().createClient();
        private final DynamoDbEnhancedClient enhancedClient =
            DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        private final String tableName;

        SyncSetupClient(String tableNameSuffix) {
            this.tableName = getConcreteTableName(tableNameSuffix);
        }

        @Override
        public <T> void createTable(TableSchema<T> schema) {
            DynamoDbTable<T> table = enhancedClient.table(tableName, schema);
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        }

        @Override
        public <T> void createTableWithGsi(TableSchema<T> schema, String indexName) {
            DynamoDbTable<T> table = enhancedClient.table(tableName, schema);
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())
                                    .globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                                                                                        .indexName(indexName)
                                                                                        .projection(p -> p.projectionType(
                                                                                            ProjectionType.KEYS_ONLY))
                                                                                        .provisionedThroughput(
                                                                                            getDefaultProvisionedThroughput())
                                                                                        .build()));
        }

        @Override
        public boolean tableExists() {
            return tableName.equals(dynamoDbClient.describeTable(r -> r.tableName(tableName)).table().tableName());
        }

        @Override
        public List<String> globalSecondaryIndexNames() {
            List<GlobalSecondaryIndexDescription> indexes =
                dynamoDbClient.describeTable(r -> r.tableName(tableName)).table().globalSecondaryIndexes();
            if (indexes == null) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>();
            for (GlobalSecondaryIndexDescription index : indexes) {
                names.add(index.indexName());
            }
            return names;
        }

        @Override
        public void deleteTable() {
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
        }
    }

    private final class AsyncSetupClient implements SetupClient {
        private final DynamoDbAsyncClient dynamoDbAsyncClient = localDynamoDb().createAsyncClient();
        private final DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
        private final String tableName;

        AsyncSetupClient(String tableNameSuffix) {
            this.tableName = getConcreteTableName(tableNameSuffix);
        }

        @Override
        public <T> void createTable(TableSchema<T> schema) {
            DynamoDbAsyncTable<T> table = enhancedClient.table(tableName, schema);
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
        }

        @Override
        public <T> void createTableWithGsi(TableSchema<T> schema, String indexName) {
            DynamoDbAsyncTable<T> table = enhancedClient.table(tableName, schema);
            table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())
                                    .globalSecondaryIndices(EnhancedGlobalSecondaryIndex.builder()
                                                                                        .indexName(indexName)
                                                                                        .projection(p -> p.projectionType(
                                                                                            ProjectionType.KEYS_ONLY))
                                                                                        .provisionedThroughput(
                                                                                            getDefaultProvisionedThroughput())
                                                                                        .build()))
                 .join();
        }

        @Override
        public boolean tableExists() {
            return tableName.equals(dynamoDbAsyncClient.describeTable(r -> r.tableName(tableName)).join().table().tableName());
        }

        @Override
        public List<String> globalSecondaryIndexNames() {
            List<GlobalSecondaryIndexDescription> indexes =
                dynamoDbAsyncClient.describeTable(r -> r.tableName(tableName)).join().table().globalSecondaryIndexes();
            if (indexes == null) {
                return Collections.emptyList();
            }
            List<String> names = new ArrayList<>();
            for (GlobalSecondaryIndexDescription index : indexes) {
                names.add(index.indexName());
            }
            return names;
        }

        @Override
        public void deleteTable() {
            dynamoDbAsyncClient.deleteTable(DeleteTableRequest.builder().tableName(tableName).build()).join();
        }
    }
}
