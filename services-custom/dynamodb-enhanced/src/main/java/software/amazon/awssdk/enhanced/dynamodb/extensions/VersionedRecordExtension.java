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

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.cleanAttributeName;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * This extension implements optimistic locking on record writes by means of a 'record version number' that is used
 * to automatically track each revision of the record as it is modified.
 * <p>
 * This extension is loaded by default when you instantiate a
 * {@link software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient} so unless you are using a custom extension
 * there is no need to specify it.
 * <p>
 * To utilize versioned record locking, first create an attribute in your model that will be used to store the record
 * version number. This attribute must be an 'integer' type numeric (long or integer), and you need to tag it as the
 * version attribute. If you are using the {@link software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema} then
 * you should use the {@link software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute}
 * annotation, otherwise if you are using the {@link software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema}
 * then you should use the {@link AttributeTags#versionAttribute()} static attribute tag.
 * <p>
 * Then, whenever a record is written the write operation will only succeed if the version number of the record has not
 * been modified since it was last read by the application. Every time a new version of the record is successfully
 * written to the database, the record version number will be automatically incremented.
 * <p>
 * <b>Version Calculation:</b> The first version written to a new record is calculated as {@code startAt + incrementBy}.
 * For example, with {@code startAt=0} and {@code incrementBy=1} (defaults), the first version is 1.
 * To start versioning from 0, use {@code startAt=-1} and {@code incrementBy=1}, which produces first version = 0.
 */
@SdkPublicApi
@ThreadSafe
public final class VersionedRecordExtension implements DynamoDbEnhancedClientExtension {
    private static final Function<String, String> VERSIONED_RECORD_EXPRESSION_VALUE_KEY_MAPPER =
        key -> ":old_" + cleanAttributeName(key) + "_value";
    private static final String CUSTOM_METADATA_KEY = "VersionedRecordExtension:VersionAttribute";
    private static final VersionAttribute VERSION_ATTRIBUTE = new VersionAttribute();

    private final long startAt;
    private final long incrementBy;

    private VersionedRecordExtension(Long startAt, Long incrementBy) {
        if (startAt != null && startAt < -1) {
            throw new IllegalArgumentException("startAt must be -1 or greater");
        }

        if (incrementBy != null && incrementBy < 1) {
            throw new IllegalArgumentException("incrementBy must be greater than 0.");
        }

        this.startAt = startAt != null ? startAt : 0L;
        this.incrementBy = incrementBy != null ? incrementBy : 1L;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class AttributeTags {
        private AttributeTags() {
        }

        public static StaticAttributeTag versionAttribute() {
            return VERSION_ATTRIBUTE;
        }

        public static StaticAttributeTag versionAttribute(Long startAt, Long incrementBy) {
            return new VersionAttribute(startAt, incrementBy);
        }
    }

    private static final class VersionAttribute implements StaticAttributeTag {
        private static final String START_AT_METADATA_KEY = "VersionedRecordExtension:StartAt";
        private static final String INCREMENT_BY_METADATA_KEY = "VersionedRecordExtension:IncrementBy";

        private final Long startAt;
        private final Long incrementBy;

        private VersionAttribute() {
            this.startAt = null;
            this.incrementBy = null;
        }

        private VersionAttribute(Long startAt, Long incrementBy) {
            this.startAt = startAt;
            this.incrementBy = incrementBy;
        }

        @Override
        public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                    AttributeValueType attributeValueType) {
            if (attributeValueType != AttributeValueType.N) {
                throw new IllegalArgumentException(String.format(
                    "Attribute '%s' of type %s is not a suitable type to be used as a version attribute. Only type 'N' " +
                    "is supported.", attributeName, attributeValueType.name()));
            }

            if (startAt != null && startAt < -1) {
                throw new IllegalArgumentException("startAt must be -1 or greater.");
            }

            if (incrementBy != null && incrementBy < 1) {
                throw new IllegalArgumentException("incrementBy must be greater than 0.");
            }

            return metadata -> metadata.addCustomMetadataObject(CUSTOM_METADATA_KEY, attributeName)
                                       .addCustomMetadataObject(START_AT_METADATA_KEY, startAt)
                                       .addCustomMetadataObject(INCREMENT_BY_METADATA_KEY, incrementBy)
                                       .markAttributeAsKey(attributeName, attributeValueType);
        }
    }

    @Override
    public WriteModification beforeWrite(DynamoDbExtensionContext.BeforeWrite context) {
        Optional<String> versionAttributeKey = context.tableMetadata()
                                                      .customMetadataObject(CUSTOM_METADATA_KEY, String.class);

        if (!versionAttributeKey.isPresent()) {
            return WriteModification.builder().build();
        }

        Map<String, AttributeValue> itemToTransform = new HashMap<>(context.items());

        String attributeKeyRef = keyRef(versionAttributeKey.get());
        AttributeValue newVersionValue;
        Expression condition;

        AttributeValue existingVersionValue = itemToTransform.get(versionAttributeKey.get());
        Long versionStartAtFromAnnotation = context.tableMetadata()
                                                   .customMetadataObject(VersionAttribute.START_AT_METADATA_KEY, Long.class)
                                                   .orElse(this.startAt);
        Long versionIncrementByFromAnnotation = context.tableMetadata()
                                                   .customMetadataObject(VersionAttribute.INCREMENT_BY_METADATA_KEY, Long.class)
                                                   .orElse(this.incrementBy);


        if (existingVersionValue == null || isNullAttributeValue(existingVersionValue)) {
            newVersionValue = AttributeValue.builder()
                                            .n(Long.toString(versionStartAtFromAnnotation + versionIncrementByFromAnnotation))
                                            .build();
            condition = Expression.builder()
                                  .expression(String.format("attribute_not_exists(%s)", attributeKeyRef))
                                  .expressionNames(Collections.singletonMap(attributeKeyRef, versionAttributeKey.get()))
                                  .build();
        } else {
            // Existing record, increment version
            if (existingVersionValue.n() == null) {
                // In this case a non-null version attribute is present, but it's not an N
                throw new IllegalArgumentException("Version attribute appears to be the wrong type. N is required.");
            }

            long existingVersion = Long.parseLong(existingVersionValue.n());
            String existingVersionValueKey = VERSIONED_RECORD_EXPRESSION_VALUE_KEY_MAPPER.apply(versionAttributeKey.get());
            long increment = versionIncrementByFromAnnotation;

            /*
            Since the new incrementBy and StartAt functionality can now accept any positive number, though unlikely
            to happen in a real life scenario, we should add overflow protection.
            */
            if (existingVersion > Long.MAX_VALUE - increment) {
                throw new IllegalStateException(
                    String.format("Version overflow detected. Current version %d + increment %d would exceed Long.MAX_VALUE",
                                  existingVersion, increment));
            }

            newVersionValue = AttributeValue.builder().n(Long.toString(existingVersion + increment)).build();

            // When version equals startAt, we can't distinguish between new and existing records
            // Use OR condition to handle both cases
            if (existingVersion == versionStartAtFromAnnotation) {
                condition = Expression.builder()
                                      .expression(String.format("attribute_not_exists(%s) OR %s = %s",
                                                              attributeKeyRef, attributeKeyRef, existingVersionValueKey))
                                      .expressionNames(Collections.singletonMap(attributeKeyRef, versionAttributeKey.get()))
                                      .expressionValues(Collections.singletonMap(existingVersionValueKey,
                                                                                 existingVersionValue))
                                      .build();
            } else {
                // Normal case - version doesn't equal startAt, must be existing record
                condition = Expression.builder()
                                      .expression(String.format("%s = %s", attributeKeyRef, existingVersionValueKey))
                                      .expressionNames(Collections.singletonMap(attributeKeyRef, versionAttributeKey.get()))
                                      .expressionValues(Collections.singletonMap(existingVersionValueKey,
                                                                                 existingVersionValue))
                                      .build();
            }
        }

        itemToTransform.put(versionAttributeKey.get(), newVersionValue);

        return WriteModification.builder()
                                .transformedItem(Collections.unmodifiableMap(itemToTransform))
                                .additionalConditionalExpression(condition)
                                .build();
    }

    @NotThreadSafe
    public static final class Builder {
        private Long startAt;
        private Long incrementBy;

        private Builder() {
        }

        /**
         * Sets the startAt used to compare if a record is the initial version of a record.
         * The first version written to a new record is calculated as {@code startAt + incrementBy}.
         * Default value - {@code 0}.
         *
         * @param startAt the starting value for version comparison, must be -1 or greater
         * @return the builder instance
         */
        public Builder startAt(Long startAt) {
            this.startAt = startAt;
            return this;
        }

        /**
         * Sets the amount to increment the version by with each subsequent update.
         * Default value - {@code 1}.
         *
         * @param incrementBy the amount to increment the version by, must be greater than 0
         * @return the builder instance
         */
        public Builder incrementBy(Long incrementBy) {
            this.incrementBy = incrementBy;
            return this;
        }

        public VersionedRecordExtension build() {
            return new VersionedRecordExtension(this.startAt, this.incrementBy);
        }
    }
}
