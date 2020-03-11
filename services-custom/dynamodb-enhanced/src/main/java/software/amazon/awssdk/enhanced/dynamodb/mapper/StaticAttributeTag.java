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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;

/**
 * Interface for a tag that can be applied to any {@link StaticAttribute}. When a tagged attribute is added to a
 * {@link software.amazon.awssdk.enhanced.dynamodb.TableSchema}, the table metadata stored on the schema will be updated
 * by calling the {@link #modifyMetadata(String, AttributeValueType)} method for every tag associated with the
 * attribute.
 * <p>
 * Common implementations of this interface that can be used to declare indices in your schema can be found in
 * {@link StaticAttributeTags}.
 */
@SdkPublicApi
public interface StaticAttributeTag {
    /**
     * A function that modifies an existing {@link StaticTableSchema.Builder} when this tag is applied to a specific
     * attribute. This will be used by the {@link StaticTableSchema} to capture all the metadata associated with
     * tagged attributes when constructing the table schema.
     *
     * @param attributeName The name of the attribute this tag has been applied to.
     * @param attributeValueType The type of the attribute this tag has been applied to. This can be used for
     *                           validation, for instance if you have an attribute tag that should only be associated
     *                           with a string.
     * @return a consumer that modifies an existing {@link StaticTableSchema.Builder}.
     */
    Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                         AttributeValueType attributeValueType);
}
