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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkPublicApi
@ThreadSafe
public final class TimeToLiveExtension implements DynamoDbEnhancedClientExtension {

    public static final String CUSTOM_METADATA_KEY = "TimeToLiveExtension:TimeToLiveAttribute";

    private TimeToLiveExtension() {
    }

    public static TimeToLiveExtension.Builder builder() {
        return new TimeToLiveExtension.Builder();
    }

    /**
     * @return an Instance of {@link TimeToLiveExtension}
     */
    public static TimeToLiveExtension create() {
        return new TimeToLiveExtension();
    }

    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
        Map<String, ?> customTTLMetadata = context.tableMetadata()
                                                  .customMetadataObject(CUSTOM_METADATA_KEY, Map.class).orElse(null);

        if (customTTLMetadata != null) {
            String ttlAttributeName = (String) customTTLMetadata.get("attributeName");
            String baseFieldName = (String) customTTLMetadata.get("baseField");
            Long duration = (Long) customTTLMetadata.get("duration");
            TemporalUnit unit = (TemporalUnit) customTTLMetadata.get("unit");

            Map<String, AttributeValue> itemToTransform = new HashMap<>(context.items());

            if (!itemToTransform.containsKey(ttlAttributeName) && StringUtils.isNotBlank(baseFieldName)
                && itemToTransform.containsKey(baseFieldName)) {
                Object baseFieldValue = context.tableSchema().converterForAttribute(baseFieldName)
                                               .transformTo(itemToTransform.get(baseFieldName));
                Long ttlEpochSeconds = computeTTLFromBase(baseFieldValue, duration, unit);
                itemToTransform.put(ttlAttributeName, AttributeValue.builder().n(String.valueOf(ttlEpochSeconds)).build());

                return WriteModification.builder().transformedItem(Collections.unmodifiableMap(itemToTransform)).build();
            }
        }

        return WriteModification.builder().build();
    }

    private static Long computeTTLFromBase(Object baseValue, long duration, TemporalUnit unit) {
        if (baseValue instanceof Instant) {
            return ((Instant) baseValue).plus(duration, unit).getEpochSecond();
        }
        if (baseValue instanceof LocalDate) {
            return ((LocalDate) baseValue).atStartOfDay(ZoneOffset.UTC).plus(duration, unit).toEpochSecond();
        }
        if (baseValue instanceof LocalDateTime) {
            return ((LocalDateTime) baseValue).plus(duration, unit).toEpochSecond(ZoneOffset.UTC);
        }
        if (baseValue instanceof LocalTime) {
            return LocalDate.now().atTime((LocalTime) baseValue).plus(duration, unit).toEpochSecond(ZoneOffset.UTC);
        }
        if (baseValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) baseValue).plus(duration, unit).toEpochSecond();
        }
        if (baseValue instanceof Long) {
            return (Long) baseValue + Duration.of(duration, unit).getSeconds();
        }

        throw new IllegalArgumentException("Unsupported base field type for TTL computation: " + baseValue.getClass().getName());
    }

    public static final class Builder {
        private Builder() {
        }

        public TimeToLiveExtension build() {
            return new TimeToLiveExtension();
        }
    }

    public static final class AttributeTags {
        private AttributeTags() {
        }

        /**
         * Used to explicitly designate an attribute to determine the TTL on the table.
         *
         * <p><b>How this works</b></p>
         * <ul>
         *   <li>If a TTL attribute is set, it takes precedence over <i>baseField</i>.</li>
         *   <li>If no TTL attribute is set, it checks for <i>baseField</i>.</li>
         *   <li>If <i>baseField</i> is present, the TTL is calculated using its value, <i>duration</i>, and <i>unit</i>.</li>
         *   <li>The final TTL value is converted to epoch seconds before storing in DynamoDB.</li>
         * </ul>
         *
         * @param baseField Optional attribute name used to determine the TTL value.
         * @param duration  Additional long value used for TTL calculation.
         * @param unit      {@link ChronoUnit} value specifying the TTL duration unit.
         */
        public static StaticAttributeTag timeToLiveAttribute(String baseField, long duration, ChronoUnit unit) {
            return new TimeToLiveAttribute(baseField, duration, unit);
        }
    }

    private static final class TimeToLiveAttribute implements StaticAttributeTag {

        public String baseField;
        public long duration;
        public ChronoUnit unit;

        private TimeToLiveAttribute(String baseField, long duration, ChronoUnit unit) {
            this.baseField = baseField;
            this.duration = duration;
            this.unit = unit;
        }

        @Override
        public <R> void validateType(String attributeName, EnhancedType<R> type,
                                     AttributeValueType attributeValueType) {

            Validate.notNull(type, "type is null");
            Validate.notNull(type.rawClass(), "rawClass is null");
            Validate.notNull(attributeValueType, "attributeValueType is null");

            if (!type.rawClass().equals(Long.class)) {
                throw new IllegalArgumentException(String.format(
                    "Attribute '%s' of type %s is not a suitable type to be used as a TTL attribute. Only type Long " +
                    "is supported.", attributeName, type.rawClass()));
            }
        }

        @Override
        public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                    AttributeValueType attributeValueType) {
            Map<String, Object> customMetadataMap = new HashMap<>();
            customMetadataMap.put("attributeName", attributeName);
            customMetadataMap.put("baseField", baseField);
            customMetadataMap.put("duration", duration);
            customMetadataMap.put("unit", unit);

            return metadata -> metadata.addCustomMetadataObject(CUSTOM_METADATA_KEY,
                                                                Collections.unmodifiableMap(customMetadataMap));
        }
    }
}
