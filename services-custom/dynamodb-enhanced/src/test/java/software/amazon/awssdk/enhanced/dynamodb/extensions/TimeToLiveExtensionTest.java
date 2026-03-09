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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.extensions.TimeToLiveExtension.AttributeTags.timeToLiveAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalDateTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LocalTimeAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZonedDateTimeAsStringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.OperationName;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class TimeToLiveExtensionTest {
    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final TableSchema<TaggedTtlItem> TAGGED_TTL_SCHEMA =
        StaticTableSchema.builder(TaggedTtlItem.class)
                         .newItemSupplier(TaggedTtlItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(TaggedTtlItem::getId)
                                                           .setter(TaggedTtlItem::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(Instant.class, a -> a.name("baseTimestamp")
                                                            .getter(TaggedTtlItem::getBaseTimestamp)
                                                            .setter(TaggedTtlItem::setBaseTimestamp))
                         .addAttribute(Long.class, a -> a.name("expiresAt")
                                                         .getter(TaggedTtlItem::getExpiresAt)
                                                         .setter(TaggedTtlItem::setExpiresAt)
                                                         .tags(timeToLiveAttribute("baseTimestamp", 5, ChronoUnit.MINUTES)))
                         .build();

    private static final TableSchema<NoTtlItem> NO_TTL_SCHEMA =
        StaticTableSchema.builder(NoTtlItem.class)
                         .newItemSupplier(NoTtlItem::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(NoTtlItem::getId)
                                                           .setter(NoTtlItem::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(Instant.class, a -> a.name("baseTimestamp")
                                                            .getter(NoTtlItem::getBaseTimestamp)
                                                            .setter(NoTtlItem::setBaseTimestamp))
                         .build();

    private final TimeToLiveExtension extension = TimeToLiveExtension.create();

    @Test
    public void builderAndCreate_returnUsableExtensionInstances() {
        assertThat(TimeToLiveExtension.builder().build()).isNotNull().isInstanceOf(TimeToLiveExtension.class);
        assertThat(TimeToLiveExtension.create()).isNotNull().isInstanceOf(TimeToLiveExtension.class);
    }

    @Test
    public void beforeWrite_withoutTtlMetadata_returnsNoTransformation() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("id-1").build());
        item.put("baseTimestamp", InstantAsStringAttributeConverter.create().transformFrom(Instant.parse("2024-01-01T00:00:00Z")));

        WriteModification result = extension.beforeWrite(defaultContext(item, NO_TTL_SCHEMA));

        assertThat(result.transformedItem()).isNull();
    }

    @Test
    public void beforeWrite_withExistingTtlValue_returnsNoTransformation() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("id-1").build());
        item.put("baseTimestamp", InstantAsStringAttributeConverter.create().transformFrom(Instant.parse("2024-01-01T00:00:00Z")));
        item.put("expiresAt", AttributeValue.builder().n("123").build());

        WriteModification result = extension.beforeWrite(defaultContext(item, TAGGED_TTL_SCHEMA));

        assertThat(result.transformedItem()).isNull();
    }

    @Test
    public void beforeWrite_withoutBaseFieldValue_returnsNoTransformation() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("id-1").build());

        WriteModification result = extension.beforeWrite(defaultContext(item, TAGGED_TTL_SCHEMA));

        assertThat(result.transformedItem()).isNull();
    }

    @Test
    public void beforeWrite_withBlankBaseField_returnsNoTransformation() {
        WriteModification result = beforeWriteWithCustomMetadata("ttl", "",
                                                                 Instant.parse("2024-01-01T00:00:00Z"),
                                                                 InstantAsStringAttributeConverter.create(), 5,
                                                                 ChronoUnit.MINUTES);

        assertThat(result.transformedItem()).isNull();
    }

    @Test
    public void beforeWrite_withTaggedSchema_computesTtlFromInstant() {
        Instant baseTime = Instant.parse("2024-01-01T00:00:00Z");
        long expectedTtl = baseTime.plus(5, ChronoUnit.MINUTES).getEpochSecond();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s("id-1").build());
        item.put("baseTimestamp", InstantAsStringAttributeConverter.create().transformFrom(baseTime));

        WriteModification result = extension.beforeWrite(defaultContext(item, TAGGED_TTL_SCHEMA));

        assertThat(item).doesNotContainKey("expiresAt");
        assertThat(result.transformedItem()).isNotNull();
        assertThat(result.transformedItem()).containsEntry("expiresAt", AttributeValue.builder().n(String.valueOf(expectedTtl)).build());
    }

    @Test
    public void beforeWrite_computesTtlFromLocalDate() {
        LocalDate baseDate = LocalDate.of(2024, 2, 1);
        long expectedTtl = baseDate.atStartOfDay(ZoneOffset.UTC).plusDays(2).toEpochSecond();

        WriteModification result = beforeWriteWithCustomMetadata("ttl", "baseField", baseDate,
                                                                 LocalDateAttributeConverter.create(), 2, ChronoUnit.DAYS);

        assertThat(ttlFrom(result, "ttl")).isEqualTo(expectedTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromLocalDateTime() {
        LocalDateTime baseDateTime = LocalDateTime.of(2024, 2, 1, 12, 30, 15);
        long expectedTtl = baseDateTime.plusHours(3).toEpochSecond(ZoneOffset.UTC);

        WriteModification result = beforeWriteWithCustomMetadata("ttl", "baseField", baseDateTime,
                                                                 LocalDateTimeAttributeConverter.create(), 3,
                                                                 ChronoUnit.HOURS);

        assertThat(ttlFrom(result, "ttl")).isEqualTo(expectedTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromLocalTime() {
        LocalTime baseTime = LocalTime.of(10, 15, 30);
        long expectedBefore = LocalDate.now().atTime(baseTime).plusMinutes(45).toEpochSecond(ZoneOffset.UTC);

        WriteModification result = beforeWriteWithCustomMetadata("ttl", "baseField", baseTime,
                                                                 LocalTimeAttributeConverter.create(), 45,
                                                                 ChronoUnit.MINUTES);

        long expectedAfter = LocalDate.now().atTime(baseTime).plusMinutes(45).toEpochSecond(ZoneOffset.UTC);
        assertThat(ttlFrom(result, "ttl")).isBetween(Math.min(expectedBefore, expectedAfter),
                                                     Math.max(expectedBefore, expectedAfter));
    }

    @Test
    public void beforeWrite_computesTtlFromZonedDateTime() {
        ZonedDateTime baseDateTime = ZonedDateTime.of(2024, 2, 1, 12, 30, 15, 0, ZoneOffset.UTC);
        long expectedTtl = baseDateTime.plusSeconds(90).toEpochSecond();

        WriteModification result = beforeWriteWithCustomMetadata("ttl", "baseField", baseDateTime,
                                                                 ZonedDateTimeAsStringAttributeConverter.create(), 90,
                                                                 ChronoUnit.SECONDS);

        assertThat(ttlFrom(result, "ttl")).isEqualTo(expectedTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromEpochSecondsLong() {
        Long baseEpochSeconds = 1_707_123_456L;
        Long expectedTtl = baseEpochSeconds + 120L;

        WriteModification result = beforeWriteWithCustomMetadata("ttl", "baseField", baseEpochSeconds,
                                                                 LongAttributeConverter.create(), 120,
                                                                 ChronoUnit.SECONDS);

        assertThat(ttlFrom(result, "ttl")).isEqualTo(expectedTtl);
    }

    @Test
    public void beforeWrite_rejectsUnsupportedBaseFieldTypes() {
        assertThatThrownBy(() -> beforeWriteWithCustomMetadata("ttl", "baseField", "not-supported",
                                                               StringAttributeConverter.create(), 1,
                                                               ChronoUnit.DAYS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported base field type for TTL computation")
            .hasMessageContaining(String.class.getName());
    }

    @Test
    public void timeToLiveAttributeTag_validateType_acceptsLong() {
        StaticAttributeTag ttlTag = timeToLiveAttribute("baseTimestamp", 10, ChronoUnit.SECONDS);

        ttlTag.validateType("expiresAt", EnhancedType.of(Long.class), AttributeValueType.N);
    }

    @Test
    public void timeToLiveAttributeTag_validateType_rejectsNonLongTypes() {
        StaticAttributeTag ttlTag = timeToLiveAttribute("baseTimestamp", 10, ChronoUnit.SECONDS);

        assertThatThrownBy(() -> ttlTag.validateType("expiresAt", EnhancedType.of(String.class), AttributeValueType.S))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Attribute 'expiresAt'")
            .hasMessageContaining("Only type Long is supported");
    }

    @Test
    public void timeToLiveAttributeTag_validateType_rejectsNullArguments() {
        StaticAttributeTag ttlTag = timeToLiveAttribute("baseTimestamp", 10, ChronoUnit.SECONDS);

        assertThatThrownBy(() -> ttlTag.validateType("expiresAt", null, AttributeValueType.N))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("type is null");

        EnhancedType<?> typeWithNullRawClass = mock(EnhancedType.class);
        when(typeWithNullRawClass.rawClass()).thenReturn(null);

        assertThatThrownBy(() -> ttlTag.validateType("expiresAt", typeWithNullRawClass, AttributeValueType.N))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("rawClass is null");

        assertThatThrownBy(() -> ttlTag.validateType("expiresAt", EnhancedType.of(Long.class), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("attributeValueType is null");
    }

    @Test
    public void timeToLiveAttributeTag_modifyMetadata_storesTtlConfiguration() {
        StaticAttributeTag ttlTag = timeToLiveAttribute("baseTimestamp", 7, ChronoUnit.HOURS);
        StaticTableMetadata.Builder metadataBuilder = StaticTableMetadata.builder();

        ttlTag.modifyMetadata("expiresAt", AttributeValueType.N).accept(metadataBuilder);

        Map<String, Object> metadata = (Map<String, Object>) metadataBuilder.build()
                                                                            .customMetadataObject(
                                                                                TimeToLiveExtension.CUSTOM_METADATA_KEY,
                                                                                Map.class)
                                                                            .orElseThrow(IllegalStateException::new);

        assertThat(metadata).containsEntry("attributeName", "expiresAt")
                            .containsEntry("baseField", "baseTimestamp")
                            .containsEntry("duration", 7L)
                            .containsEntry("unit", ChronoUnit.HOURS);
    }

    private WriteModification beforeWriteWithCustomMetadata(String ttlAttributeName,
                                                            String baseFieldName,
                                                            Object baseFieldValue,
                                                            AttributeConverter converter,
                                                            long duration,
                                                            ChronoUnit unit) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, converter.transformFrom(baseFieldValue));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttributeName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema tableSchema = mock(TableSchema.class);
        when(tableSchema.converterForAttribute(baseFieldName)).thenReturn(converter);

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(tableSchema);

        return extension.beforeWrite(context);
    }

    private long ttlFrom(WriteModification result, String attributeName) {
        assertThat(result.transformedItem()).isNotNull().containsKey(attributeName);
        return Long.parseLong(result.transformedItem().get(attributeName).n());
    }

    private DynamoDbExtensionContext.BeforeWrite defaultContext(Map<String, AttributeValue> item, TableSchema<?> tableSchema) {
        return DefaultDynamoDbExtensionContext.builder()
                                              .items(item)
                                              .tableSchema(tableSchema)
                                              .tableMetadata(tableSchema.tableMetadata())
                                              .operationName(OperationName.UPDATE_ITEM)
                                              .operationContext(PRIMARY_CONTEXT)
                                              .build();
    }

    private static final class TaggedTtlItem {
        private String id;
        private Instant baseTimestamp;
        private Long expiresAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Instant getBaseTimestamp() {
            return baseTimestamp;
        }

        public void setBaseTimestamp(Instant baseTimestamp) {
            this.baseTimestamp = baseTimestamp;
        }

        public Long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(Long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    private static final class NoTtlItem {
        private String id;
        private Instant baseTimestamp;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Instant getBaseTimestamp() {
            return baseTimestamp;
        }

        public void setBaseTimestamp(Instant baseTimestamp) {
            this.baseTimestamp = baseTimestamp;
        }
    }
}
