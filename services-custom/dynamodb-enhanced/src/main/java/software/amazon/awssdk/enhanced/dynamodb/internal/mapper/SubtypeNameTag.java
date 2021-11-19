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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;

@SdkInternalApi
public class SubtypeNameTag implements StaticAttributeTag {
    private static final SubtypeNameTag INSTANCE = new SubtypeNameTag();
    private static final String CUSTOM_METADATA_KEY = "SubtypeName";

    private SubtypeNameTag() {
    }

    public static Optional<String> resolve(TableMetadata tableMetadata) {
        return tableMetadata.customMetadataObject(CUSTOM_METADATA_KEY, String.class);
    }

    @Override
    public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                AttributeValueType attributeValueType) {
        if (!AttributeValueType.S.equals(attributeValueType)) {
            throw new IllegalArgumentException(
                String.format("Attribute '%s' of type %s is not a suitable type to be used as a subtype name. Only string is "
                              + "supported for this purpose.", attributeName, attributeValueType.name()));
        }

        return metadata ->
                metadata.addCustomMetadataObject(CUSTOM_METADATA_KEY, attributeName);
    }

    public static SubtypeNameTag create() {
        return INSTANCE;
    }
}
