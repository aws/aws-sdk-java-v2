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

import static software.amazon.awssdk.enhanced.dynamodb.extensions.TimeToLiveExtension.AttributeTags.timeToLiveAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithTTL;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithTTLImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticImmutableTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans.SimpleImmutable;

final class TimeToLiveSchemaVariants {
    private static final TableSchema<RecordWithTTL> STATIC_TTL_BEAN_SCHEMA =
        StaticTableSchema.builder(RecordWithTTL.class)
                         .newItemSupplier(RecordWithTTL::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(RecordWithTTL::getId)
                                                           .setter(RecordWithTTL::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("attribute")
                                                           .getter(RecordWithTTL::getAttribute)
                                                           .setter(RecordWithTTL::setAttribute))
                         .addAttribute(Instant.class, a -> a.name("updatedDate")
                                                            .getter(RecordWithTTL::getUpdatedDate)
                                                            .setter(RecordWithTTL::setUpdatedDate))
                         .addAttribute(Long.class, a -> a.name("expirationDate")
                                                         .getter(RecordWithTTL::getExpirationDate)
                                                         .setter(RecordWithTTL::setExpirationDate)
                                                         .tags(timeToLiveAttribute("updatedDate", 30, ChronoUnit.DAYS)))
                         .build();

    private static final TableSchema<SimpleBean> STATIC_BEAN_SCHEMA_WITHOUT_TTL =
        StaticTableSchema.builder(SimpleBean.class)
                         .newItemSupplier(SimpleBean::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(SimpleBean::getId)
                                                           .setter(SimpleBean::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(Integer.class, a -> a.name("integerAttribute")
                                                            .getter(SimpleBean::getIntegerAttribute)
                                                            .setter(SimpleBean::setIntegerAttribute))
                         .build();

    private static final TableSchema<RecordWithTTLImmutable> STATIC_TTL_IMMUTABLE_SCHEMA =
        StaticImmutableTableSchema.builder(RecordWithTTLImmutable.class, RecordWithTTLImmutable.Builder.class)
                                  .newItemBuilder(RecordWithTTLImmutable::builder,
                                                  RecordWithTTLImmutable.Builder::build)
                                  .addAttribute(String.class, a -> a.name("id")
                                                            .getter(RecordWithTTLImmutable::id)
                                                            .setter(RecordWithTTLImmutable.Builder::id)
                                                            .tags(primaryPartitionKey()))
                                  .addAttribute(Long.class, a -> a.name("expirationDate")
                                                          .getter(RecordWithTTLImmutable::expirationDate)
                                                          .setter(RecordWithTTLImmutable.Builder::expirationDate)
                                                          .tags(timeToLiveAttribute("", 0, ChronoUnit.SECONDS)))
                                  .build();

    private static final TableSchema<SimpleImmutable> STATIC_IMMUTABLE_SCHEMA_WITHOUT_TTL =
        StaticImmutableTableSchema.builder(SimpleImmutable.class, SimpleImmutable.Builder.class)
                                  .newItemBuilder(SimpleImmutable::builder, SimpleImmutable.Builder::build)
                                  .addAttribute(String.class, a -> a.name("id")
                                                            .getter(SimpleImmutable::id)
                                                            .setter(SimpleImmutable.Builder::id)
                                                            .tags(primaryPartitionKey()))
                                  .addAttribute(Integer.class, a -> a.name("integerAttribute")
                                                             .getter(SimpleImmutable::integerAttribute)
                                                             .setter(SimpleImmutable.Builder::integerAttribute))
                                  .build();

    private TimeToLiveSchemaVariants() {
    }

    static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"@DynamoDbBean", TableSchema.fromBean(RecordWithTTL.class), TableSchema.fromBean(SimpleBean.class)},
            {"@DynamoDbImmutable", TableSchema.fromImmutableClass(RecordWithTTLImmutable.class),
                TableSchema.fromImmutableClass(SimpleImmutable.class)},
            {"StaticTableSchema", STATIC_TTL_BEAN_SCHEMA, STATIC_BEAN_SCHEMA_WITHOUT_TTL},
            {"StaticImmutableTableSchema", STATIC_TTL_IMMUTABLE_SCHEMA, STATIC_IMMUTABLE_SCHEMA_WITHOUT_TTL}
        });
    }
}

