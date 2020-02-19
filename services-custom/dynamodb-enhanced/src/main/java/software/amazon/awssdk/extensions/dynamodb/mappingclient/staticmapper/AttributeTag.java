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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper;

import java.util.Map;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.AttributeValueType;

/**
 * Extend this class to create your own attribute tag that can be used with extensions.
 */
@SdkPublicApi
public abstract class AttributeTag {
    /**
     * Attribute tags are recorded in the {@link software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata}
     * object as custom metadata objects with a string key and a flexible value type. This method will be called
     * whenever this object is used to tag an attribute in a {@link StaticTableSchema}.
     *
     * @param attributeName The name of the attribute this tag has been applied to.
     * @param attributeValueType The type of the attribute this tag has been applied to. This can be used for
     *                           validation, for instance if you have an attribute tag that should only be associated
     *                           with a string.
     * @return A map of custom metadata map entries that will be added to the custom metadata stored in the
     * {@link software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata} object for the table being mapped.
     * Your extension should know what to do with these custom metadata entries.
     */
    protected abstract Map<String, Object> customMetadataForAttribute(String attributeName,
                                                                      AttributeValueType attributeValueType);

    /**
     * Returns a boolean that indicates whether this attribute tag qualifies the attribute that has been tagged with
     * it as a 'key attribute'. This is used by other extensions to determine how the attribute should be treated,
     * for instance the encryption extension will not encrypt any attributes marked as 'key attributes' by default.
     * @return true if this attribute should be considered a 'key attribute'; false if not.
     */
    protected abstract boolean isKeyAttribute();

    void setTableMetadataForAttribute(String attributeName,
                                      AttributeValueType attributeValueType,
                                      StaticTableMetadata.Builder tableMetadataBuilder) {
        if (isKeyAttribute()) {
            tableMetadataBuilder.markAttributeAsKey(attributeName, attributeValueType);
        }

        customMetadataForAttribute(attributeName, attributeValueType)
            .forEach(tableMetadataBuilder::addCustomMetadataObject);
    }
}
