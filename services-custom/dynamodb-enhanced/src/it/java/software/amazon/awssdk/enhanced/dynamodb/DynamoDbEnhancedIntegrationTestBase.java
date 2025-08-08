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

package software.amazon.awssdk.enhanced.dynamodb;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Record;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public abstract class DynamoDbEnhancedIntegrationTestBase extends AwsIntegrationTestBase {
    protected static String createTestTableName() {
        return UUID.randomUUID() + "-ddb-enhanced-integ-test";
    }

    protected static DynamoDbClient createDynamoDbClient() {
        return DynamoDbClient.builder()
                             .credentialsProvider(getCredentialsProvider())
                             .build();
    }

    protected static DynamoDbAsyncClient createAsyncDynamoDbClient() {
        return DynamoDbAsyncClient.builder()
                                  .credentialsProvider(getCredentialsProvider())
                                  .build();
    }

    protected static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey(), secondaryPartitionKey("index1")))
                         .addAttribute(Integer.class, a -> a.name("sort")
                                                            .getter(Record::getSort)
                                                            .setter(Record::setSort)
                                                            .tags(primarySortKey(), secondarySortKey("index1")))
                         .addAttribute(Integer.class, a -> a.name("value")
                                                            .getter(Record::getValue)
                                                            .setter(Record::setValue))
                         .addAttribute(String.class, a -> a.name("gsi_id")
                                                           .getter(Record::getGsiId)
                                                           .setter(Record::setGsiId)
                                                           .tags(secondaryPartitionKey("gsi_keys_only")))
                         .addAttribute(Integer.class, a -> a.name("gsi_sort")
                                                            .getter(Record::getGsiSort)
                                                            .setter(Record::setGsiSort)
                                                            .tags(secondarySortKey("gsi_keys_only")))
                         .addAttribute(String.class, a -> a.name("stringAttribute")
                                                           .getter(Record::getStringAttribute)
                                                           .setter(Record::setStringAttribute))
                         .build();

    protected static final TableSchema<Record> RECORD_WITH_FLATTEN_MAP_TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey(), secondaryPartitionKey("index1")))
                         .addAttribute(Integer.class, a -> a.name("sort")
                                                            .getter(Record::getSort)
                                                            .setter(Record::setSort)
                                                            .tags(primarySortKey(), secondarySortKey("index1")))
                         .addAttribute(Integer.class, a -> a.name("value")
                                                            .getter(Record::getValue)
                                                            .setter(Record::setValue))
                         .addAttribute(String.class, a -> a.name("gsi_id")
                                                           .getter(Record::getGsiId)
                                                           .setter(Record::setGsiId)
                                                           .tags(secondaryPartitionKey("gsi_keys_only")))
                         .addAttribute(Integer.class, a -> a.name("gsi_sort")
                                                            .getter(Record::getGsiSort)
                                                            .setter(Record::setGsiSort)
                                                            .tags(secondarySortKey("gsi_keys_only")))
                         .addAttribute(String.class, a -> a.name("stringAttribute")
                                                           .getter(Record::getStringAttribute)
                                                           .setter(Record::setStringAttribute))
                         .flatten("attributesMap",
                                  Record::getAttributesMap,
                                  Record::setAttributesMap)
                         .build();


    protected static final List<Record> RECORDS =
        IntStream.range(0, 9)
                 .mapToObj(i -> new Record()
                     .setId("id-value")
                     .setSort(i)
                     .setValue(i)
                     .setStringAttribute(getStringAttrValue(10 * 1024))
                     .setGsiId("gsi-id-value")
                     .setGsiSort(i))
                 .collect(Collectors.toList());

    protected static final List<Record> RECORDS_WITH_FLATTEN_MAP =
        IntStream.range(0, 9)
                 .mapToObj(i -> new Record()
                     .setId("id-value")
                     .setSort(i)
                     .setValue(i)
                     .setStringAttribute(getStringAttrValue(10 * 1024))
                     .setGsiId("gsi-id-value")
                     .setGsiSort(i)
                     .setAttributesMap(new HashMap<String, String>() {{
                         put("mapAttribute1", "mapValue1");
                         put("mapAttribute2", "mapValue2");
                         put("mapAttribute3", "mapValue3");
                     }}))
                 .collect(Collectors.toList());

    protected static final List<Record> KEYS_ONLY_RECORDS =
        RECORDS.stream()
               .map(record -> new Record()
                   .setId(record.getId())
                   .setSort(record.getSort())
                   .setGsiId(record.getGsiId())
                   .setGsiSort(record.getGsiSort()))
               .collect(Collectors.toList());

    protected static String getStringAttrValue(int numChars) {
        char[] chars = new char[numChars];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }

}
