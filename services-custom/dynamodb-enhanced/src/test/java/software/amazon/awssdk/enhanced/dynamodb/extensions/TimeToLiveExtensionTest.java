package software.amazon.awssdk.enhanced.dynamodb.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithDefaultTTL;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithSimpleTTL;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.RecordWithTTL;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class TimeToLiveExtensionTest {

    private static final String TABLE_NAME = "table-name";
    private static final OperationContext PRIMARY_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final TableSchema<RecordWithTTL> TABLE_SCHEMA =
        TableSchema.fromClass(RecordWithTTL.class);

    private final TimeToLiveExtension timeToLiveExtension = TimeToLiveExtension.create();

    @Test
    public void beforeWrite_addsTtlAttributeIfNotPresent() {
        String ttlAttrName = "expirationDate";
        String baseFieldName = "updatedDate";
        long duration = 30L;
        ChronoUnit unit = ChronoUnit.DAYS;

        Instant baseTime = Instant.now();
        long expectedTtl = baseTime.plus(duration, unit).getEpochSecond();

        Map<String, AttributeValue> items = new HashMap<>();
        items.put(baseFieldName, InstantAsStringAttributeConverter.create().transformFrom(baseTime));

        WriteModification result =
            timeToLiveExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                           .items(items)
                                                                           .tableSchema(TABLE_SCHEMA)
                                                                           .tableMetadata(TABLE_SCHEMA.tableMetadata())
                                                                           .operationName(OperationName.UPDATE_ITEM)
                                                                           .operationContext(PRIMARY_CONTEXT).build());

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(expectedTtl, actualTtl);
    }

    @Test
    public void beforeWrite_skipsIfTtlAlreadyPresent() {
        String ttlAttrName = "expirationDate";

        Map<String, AttributeValue> items = new HashMap<>();
        items.put(ttlAttrName, AttributeValue.fromN("12345")); // already present

        WriteModification result =
            timeToLiveExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                           .items(items)
                                                                           .tableSchema(TABLE_SCHEMA)
                                                                           .tableMetadata(TABLE_SCHEMA.tableMetadata())
                                                                           .operationName(OperationName.UPDATE_ITEM)
                                                                           .operationContext(PRIMARY_CONTEXT).build());

        // TTL was already present, nothing modified
        assertNull(result.transformedItem());
    }

    @Test
    public void beforeWrite_skipsIfTtlNotPresentAndBaseFieldEmpty() {
        Map<String, AttributeValue> items = new HashMap<>();
        items.put("attribute", AttributeValue.fromN("attributeValue"));

        TableSchema<RecordWithSimpleTTL> simpleTTLTableSchema = TableSchema.fromClass(RecordWithSimpleTTL.class);

        WriteModification result =
            timeToLiveExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                           .items(items)
                                                                           .tableSchema(simpleTTLTableSchema)
                                                                           .tableMetadata(simpleTTLTableSchema.tableMetadata())
                                                                           .operationName(OperationName.UPDATE_ITEM)
                                                                           .operationContext(PRIMARY_CONTEXT).build());

        // TTL not present, but no baseField to compute the TTL value
        assertNull(result.transformedItem());
    }

    @Test
    public void beforeWrite_addsTtlAttributeWithDefaults() {
        String ttlAttrName = "expirationDate";
        String baseFieldName = "updatedDate";

        Map<String, AttributeValue> items = new HashMap<>();
        items.put("id", AttributeValue.fromN("id123"));
        items.put("attribute", AttributeValue.fromN("attributeValue"));
        items.put(baseFieldName, InstantAsStringAttributeConverter.create().transformFrom(Instant.now()));

        TableSchema<RecordWithDefaultTTL> defaultTTLTableSchema = TableSchema.fromClass(RecordWithDefaultTTL.class);

        WriteModification result =
            timeToLiveExtension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                           .items(items)
                                                                           .tableSchema(defaultTTLTableSchema)
                                                                           .tableMetadata(defaultTTLTableSchema.tableMetadata())
                                                                           .operationName(OperationName.UPDATE_ITEM)
                                                                           .operationContext(PRIMARY_CONTEXT).build());

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        // TTL equals baseField, since duration and unit are not specified (default 0 and SECONDS)
        long updatedDate = Instant.parse(transformed.get(baseFieldName).s()).getEpochSecond();
        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(updatedDate, actualTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromLocalDate() {
        String ttlAttrName = "ttl";
        String baseFieldName = "createdAt";
        long duration = 1L;
        ChronoUnit unit = ChronoUnit.DAYS;

        LocalDate baseTime = LocalDate.of(2024, 1, 1);
        long expectedTtl = baseTime.atStartOfDay(ZoneOffset.UTC).plus(duration, unit).toEpochSecond();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, LocalDateAttributeConverter.create().transformFrom(baseTime));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttrName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema schema = mock(TableSchema.class);
        when(schema.converterForAttribute(baseFieldName)).thenReturn(LocalDateAttributeConverter.create());

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(schema);

        TimeToLiveExtension extension = TimeToLiveExtension.create();
        WriteModification result = extension.beforeWrite(context);

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(expectedTtl, actualTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromLocalDateTime() {
        String ttlAttrName = "ttl";
        String baseFieldName = "createdAt";
        long duration = 2L;
        ChronoUnit unit = ChronoUnit.HOURS;

        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        long expectedTtl = baseTime.plusHours(2).toEpochSecond(ZoneOffset.UTC);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, LocalDateTimeAttributeConverter.create().transformFrom(baseTime));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttrName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema schema = mock(TableSchema.class);
        when(schema.converterForAttribute(baseFieldName)).thenReturn(LocalDateTimeAttributeConverter.create());

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(schema);

        TimeToLiveExtension extension = TimeToLiveExtension.create();
        WriteModification result = extension.beforeWrite(context);

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(expectedTtl, actualTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromLocalTime() {
        String ttlAttrName = "ttl";
        String baseFieldName = "createdAt";
        long duration = 30L;
        ChronoUnit unit = ChronoUnit.MINUTES;

        LocalTime baseTime = LocalTime.of(10, 0);
        long expectedTtl = LocalDate.now().atTime(baseTime).plusMinutes(30).toEpochSecond(ZoneOffset.UTC);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, LocalTimeAttributeConverter.create().transformFrom(baseTime));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttrName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema schema = mock(TableSchema.class);
        when(schema.converterForAttribute(baseFieldName)).thenReturn(LocalTimeAttributeConverter.create());

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(schema);

        TimeToLiveExtension extension = TimeToLiveExtension.create();
        WriteModification result = extension.beforeWrite(context);

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(expectedTtl, actualTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromZonedDateTime() {
        String ttlAttrName = "ttl";
        String baseFieldName = "createdAt";
        long duration = 15L;
        ChronoUnit unit = ChronoUnit.MINUTES;

        ZonedDateTime baseTime = ZonedDateTime.now(ZoneOffset.UTC);
        long expectedTtl = baseTime.plusMinutes(15).toEpochSecond();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, ZonedDateTimeAsStringAttributeConverter.create().transformFrom(baseTime));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttrName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema schema = mock(TableSchema.class);
        when(schema.converterForAttribute(baseFieldName)).thenReturn(ZonedDateTimeAsStringAttributeConverter.create());

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(schema);

        TimeToLiveExtension extension = TimeToLiveExtension.create();
        WriteModification result = extension.beforeWrite(context);

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(expectedTtl, actualTtl);
    }

    @Test
    public void beforeWrite_computesTtlFromLong() {
        String ttlAttrName = "ttl";
        String baseFieldName = "createdAt";
        long duration = 120L;
        ChronoUnit unit = ChronoUnit.SECONDS;

        Long baseTime = Instant.now().getEpochSecond();
        long expectedTtl = baseTime + 120;

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, LongAttributeConverter.create().transformFrom(baseTime));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttrName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema schema = mock(TableSchema.class);
        when(schema.converterForAttribute(baseFieldName)).thenReturn(LongAttributeConverter.create());

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(schema);

        TimeToLiveExtension extension = TimeToLiveExtension.create();
        WriteModification result = extension.beforeWrite(context);

        Map<String, AttributeValue> transformed = result.transformedItem();
        assertNotNull(transformed);
        assertTrue(transformed.containsKey(ttlAttrName));

        long actualTtl = Long.parseLong(transformed.get(ttlAttrName).n());
        assertEquals(expectedTtl, actualTtl);
    }

    @Test
    public void beforeWrite_computesTtlThrowsExceptionForUnsupportedType() {
        String ttlAttrName = "ttl";
        String baseFieldName = "createdAt";
        long duration = 60L;
        ChronoUnit unit = ChronoUnit.SECONDS;

        String baseTime = "invalidType";

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(baseFieldName, StringAttributeConverter.create().transformFrom(baseTime));

        Map<String, Object> customMetadata = new HashMap<>();
        customMetadata.put("attributeName", ttlAttrName);
        customMetadata.put("baseField", baseFieldName);
        customMetadata.put("duration", duration);
        customMetadata.put("unit", unit);

        TableMetadata tableMetadata = mock(TableMetadata.class);
        when(tableMetadata.customMetadataObject(TimeToLiveExtension.CUSTOM_METADATA_KEY, Map.class))
            .thenReturn(Optional.of(customMetadata));

        TableSchema schema = mock(TableSchema.class);
        when(schema.converterForAttribute(baseFieldName)).thenReturn(StringAttributeConverter.create());

        DynamoDbExtensionContext.BeforeWrite context = mock(DynamoDbExtensionContext.BeforeWrite.class);
        when(context.items()).thenReturn(item);
        when(context.tableMetadata()).thenReturn(tableMetadata);
        when(context.tableSchema()).thenReturn(schema);

        TimeToLiveExtension extension = TimeToLiveExtension.create();

        assertThrows(IllegalArgumentException.class, () ->
            extension.beforeWrite(context));
    }
}
