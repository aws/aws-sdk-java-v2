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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTag;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;

@SdkInternalApi
public class UpdateBehaviorTag implements StaticAttributeTag {
    private static final String CUSTOM_METADATA_KEY_PREFIX = "UpdateBehavior:";
    private static final UpdateBehavior DEFAULT_UPDATE_BEHAVIOR = UpdateBehavior.WRITE_ALWAYS;
    private static final UpdateBehaviorTag WRITE_ALWAYS_TAG = new UpdateBehaviorTag(UpdateBehavior.WRITE_ALWAYS);
    private static final UpdateBehaviorTag WRITE_IF_NOT_EXISTS_TAG =
            new UpdateBehaviorTag(UpdateBehavior.WRITE_IF_NOT_EXISTS);

    private final UpdateBehavior updateBehavior;

    private UpdateBehaviorTag(UpdateBehavior updateBehavior) {
        this.updateBehavior = updateBehavior;
    }

    public static UpdateBehaviorTag fromUpdateBehavior(UpdateBehavior updateBehavior) {
        switch (updateBehavior) {
            case WRITE_ALWAYS:
                return WRITE_ALWAYS_TAG;
            case WRITE_IF_NOT_EXISTS:
                return WRITE_IF_NOT_EXISTS_TAG;
            default:
                throw new IllegalArgumentException("Update behavior '" + updateBehavior + "' not supported");
        }
    }

    public static UpdateBehavior resolveForAttribute(String attributeName, TableMetadata tableMetadata) {
        String metadataKey = CUSTOM_METADATA_KEY_PREFIX + attributeName;
        return tableMetadata.customMetadataObject(metadataKey, UpdateBehavior.class).orElse(DEFAULT_UPDATE_BEHAVIOR);
    }

    @Override
    public Consumer<StaticTableMetadata.Builder> modifyMetadata(String attributeName,
                                                                AttributeValueType attributeValueType) {
        return metadata ->
                metadata.addCustomMetadataObject(CUSTOM_METADATA_KEY_PREFIX + attributeName, this.updateBehavior);
    }
}
