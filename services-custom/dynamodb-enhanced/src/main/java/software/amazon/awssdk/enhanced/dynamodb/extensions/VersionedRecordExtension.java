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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

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
public final class VersionedRecordExtension implements DynamoDbEnhancedClientExtension {
    private static final Function<String, String> EXPRESSION_KEY_MAPPER = key -> ":old_" + key + "_value";
    private static final String CUSTOM_METADATA_KEY = "VersionedRecordExtension:VersionAttribute";
    private static final VersionAttribute VERSION_ATTRIBUTE = new VersionAttribute();

    private VersionedRecordExtension() {
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
            if (!AttributeValueType.N.equals(attributeValueType)) {
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

        Map<String, AttributeValue> itemToTransform = new HashMap<>(context.items());
        AttributeValue newVersionValue;
        Expression condition;
        Optional<AttributeValue> existingVersionValue =
            Optional.ofNullable(itemToTransform.get(versionAttributeKey.get()));

        if (!existingVersionValue.isPresent() || isNullAttributeValue(existingVersionValue.get())) {
            // First version of the record
            newVersionValue = AttributeValue.builder().n("1").build();
            condition = Expression.builder()
                                  .expression(String.format("attribute_not_exists(%s)", versionAttributeKey.get()))
                                  .build();
        } else {
            // Existing record, increment version
            if (existingVersionValue.get().n() == null) {
                // In this case a non-null version attribute is present, but it's not an N
                throw new IllegalArgumentException("Version attribute appears to be the wrong type. N is required.");
            }

            int existingVersion = Integer.parseInt(existingVersionValue.get().n());
            String existingVersionValueKey = EXPRESSION_KEY_MAPPER.apply(versionAttributeKey.get());
            newVersionValue = AttributeValue.builder().n(Integer.toString(existingVersion + 1)).build();
            condition = Expression.builder()
                                  .expression(String.format("%s = %s", versionAttributeKey.get(),
                                                            existingVersionValueKey))
                                  .expressionValues(Collections.singletonMap(existingVersionValueKey,
                                                                             existingVersionValue.get()))
                                  .build();
        }

        itemToTransform.put(versionAttributeKey.get(), newVersionValue);

        return WriteModification.builder()
                                .transformedItem(Collections.unmodifiableMap(itemToTransform))
                                .additionalConditionalExpression(condition)
                                .build();
    }

    public static final class Builder {
        private Builder() {
        }

        public VersionedRecordExtension build() {
            return new VersionedRecordExtension();
        }
    }
}
