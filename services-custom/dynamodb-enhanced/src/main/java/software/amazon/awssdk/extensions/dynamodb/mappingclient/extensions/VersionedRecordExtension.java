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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.isNullAttributeValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.AttributeValueType;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTag;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * This extension implements optimistic locking on record writes by means of a 'record version number' that is used
 * to automatically track each revision of the record as it is modified. To use this extension, first load it as part
 * of your MappedTable instantiation:
 *
 * MappedTable.builder()
 *            .extendWith(VersionedRecordExtension.builder().build())
 *            .build();
 *
 * Then create an attribute in your model that will be used to store the record version number. This attribute must
 * be an 'integer' type numeric (long or integer), and you need to tag it as the version attribute:
 *
 * ..., integerNumber("version", Customer::getVersion, Customer::setVersion).as(version()), ...
 *
 * Then, whenever a record is written the write operation will only succeed if the version number of the record has not
 * been modified since it was last read by the application. Every time a new version of the record is successfully
 * written to the database, the record version number will be automatically incremented.
 */
@SdkPublicApi
public class VersionedRecordExtension implements MapperExtension {
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

        public static AttributeTag version() {
            return VERSION_ATTRIBUTE;
        }
    }

    private static class VersionAttribute extends AttributeTag {
        @Override
        protected boolean isKeyAttribute() {
            return true;
        }

        @Override
        public Map<String, Object> customMetadataForAttribute(String attributeName,
                                                              AttributeValueType attributeValueType) {
            if (!AttributeValueType.N.equals(attributeValueType)) {
                throw new IllegalArgumentException(String.format("Attribute '%s' of type %s is not a suitable type to"
                    + " be used as a version attribute. Only type 'N' is supported.", attributeName,
                                                                 attributeValueType.name()));
            }

            return Collections.singletonMap(CUSTOM_METADATA_KEY, attributeName);
        }
    }

    @Override
    public WriteModification beforeWrite(Map<String, AttributeValue> item,
                                         OperationContext operationContext,
                                         TableMetadata tableMetadata) {
        Optional<String> versionAttributeKey = tableMetadata.customMetadataObject(CUSTOM_METADATA_KEY, String.class);

        if (!versionAttributeKey.isPresent()) {
            return WriteModification.builder().build();
        }

        Map<String, AttributeValue> itemToTransform = new HashMap<>(item);
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
