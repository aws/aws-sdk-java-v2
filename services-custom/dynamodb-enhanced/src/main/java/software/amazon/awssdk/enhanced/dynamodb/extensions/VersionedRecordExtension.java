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
import software.amazon.awssdk.utils.Pair;

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
 */
@SdkPublicApi
@ThreadSafe
public final class VersionedRecordExtension implements DynamoDbEnhancedClientExtension {
    private static final Function<String, String> VERSIONED_RECORD_EXPRESSION_VALUE_KEY_MAPPER = key -> ":old_" + key + "_value";
    private static final String CUSTOM_METADATA_KEY = "VersionedRecordExtension:VersionAttribute";
    private static final VersionAttribute VERSION_ATTRIBUTE = new VersionAttribute();
    private static final AttributeValue DEFAULT_VALUE = AttributeValue.fromNul(Boolean.TRUE);

    private final int startingValue;
    private final int increment;

    /**
     * Creates a new {@link VersionedRecordExtension} using the supplied starting and incrementing value.
     *
     * @param startingValue the value used to compare if a record is the initial version of a record.
     * @param increment the amount to increment the version by with each subsequent update.
     */
    private VersionedRecordExtension(int startingValue, int increment) {
        this.startingValue = startingValue;
        this.increment = increment;
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
    }

    private static class VersionAttribute implements StaticAttributeTag {
        @Override
        public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                    AttributeValueType attributeValueType) {
            if (attributeValueType != AttributeValueType.N) {
                throw new IllegalArgumentException(String.format(
                    "Attribute '%s' of type %s is not a suitable type to be used as a version attribute. Only type 'N' " +
                        "is supported.", attributeName, attributeValueType.name()));
            }

            return metadata -> metadata.addCustomMetadataObject(CUSTOM_METADATA_KEY, attributeName)
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

        Pair<AttributeValue, Expression> updates = getRecordUpdates(versionAttributeKey.get(), context.items());

        // Unpack values from Pair
        AttributeValue newVersionValue = updates.left();
        Expression condition = updates.right();

        Map<String, AttributeValue> itemToTransform = new HashMap<>(context.items());
        itemToTransform.put(versionAttributeKey.get(), newVersionValue);

        return WriteModification.builder()
                                .transformedItem(Collections.unmodifiableMap(itemToTransform))
                                .additionalConditionalExpression(condition)
                                .build();
    }

    private Pair<AttributeValue, Expression> getRecordUpdates(String versionAttributeKey,
                                                              Map<String, AttributeValue> itemToTransform) {
        // Default to NUL if not present to reduce additional checks further along
        AttributeValue existingVersionValue = itemToTransform.getOrDefault(versionAttributeKey, DEFAULT_VALUE);

        if (isInitialVersion(existingVersionValue)) {
            // First version of the record ensure it does not exist
            return createInitialRecord(versionAttributeKey);
        }
        // Existing record, increment version
        return updateExistingRecord(versionAttributeKey, existingVersionValue);
    }

    private boolean isInitialVersion(AttributeValue existingVersionValue) {
        return isNullAttributeValue(existingVersionValue)
               || getExistingVersion(existingVersionValue) == this.startingValue;
    }

    private Pair<AttributeValue, Expression> createInitialRecord(String versionAttributeKey) {
        AttributeValue newVersionValue = incrementVersion(this.startingValue);

        String attributeKeyRef = keyRef(versionAttributeKey);

        Expression condition = Expression.builder()
                                         // Check that the version does not exist before setting the initial value.
                                         .expression(String.format("attribute_not_exists(%s)", attributeKeyRef))
                                         .expressionNames(Collections.singletonMap(attributeKeyRef, versionAttributeKey))
                                         .build();

        return Pair.of(newVersionValue, condition);
    }

    private Pair<AttributeValue, Expression> updateExistingRecord(String versionAttributeKey,
                                                                  AttributeValue existingVersionValue) {
        int existingVersion = getExistingVersion(existingVersionValue);
        AttributeValue newVersionValue = incrementVersion(existingVersion);

        String attributeKeyRef = keyRef(versionAttributeKey);
        String existingVersionValueKey = VERSIONED_RECORD_EXPRESSION_VALUE_KEY_MAPPER.apply(versionAttributeKey);

        Expression condition = Expression.builder()
                                         // Check that the version matches the existing value before setting the updated value.
                                         .expression(String.format("%s = %s", attributeKeyRef, existingVersionValueKey))
                                         .expressionNames(Collections.singletonMap(attributeKeyRef, versionAttributeKey))
                                         .expressionValues(Collections.singletonMap(existingVersionValueKey,
                                                                                    existingVersionValue))
                                         .build();

        return Pair.of(newVersionValue, condition);
    }

    private int getExistingVersion(AttributeValue existingVersionValue) {
        if (existingVersionValue.n() == null) {
            // In this case a non-null version attribute is present, but it's not an N
            throw new IllegalArgumentException("Version attribute appears to be the wrong type. N is required.");
        }

        return Integer.parseInt(existingVersionValue.n());
    }

    private AttributeValue incrementVersion(int version) {
        return AttributeValue.fromN(Integer.toString(version + this.increment));
    }

    @NotThreadSafe
    public static final class Builder {
        private int startingValue = 0;
        private int increment = 1;

        private Builder() {
        }

        /**
         * Sets the startingValue used to compare if a record is the initial version of a record.
         * Default value - {@code 0}.
         *
         * @param startingValue
         * @return the builder instance
         */
        public Builder startAt(int startingValue) {
            this.startingValue = startingValue;
            return this;
        }

        /**
         * Sets the amount to increment the version by with each subsequent update.
         * Default value - {@code 1}.
         *
         * @param increment
         * @return the builder instance
         */
        public Builder incrementBy(int increment) {
            this.increment = increment;
            return this;
        }

        public VersionedRecordExtension build() {
            return new VersionedRecordExtension(this.startingValue, this.increment);
        }
    }
}
