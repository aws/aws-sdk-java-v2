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

/**
 * Extend this class to create your own table tag that can be used with extensions.
 */
@SdkPublicApi
public abstract class TableTag {
    /**
     * Table tags are recorded in the {@link software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata}
     * object as custom metadata objects with a string key and a flexible value type. This method will be called
     * whenever this object is used to tag a table in a {@link StaticTableSchema}.
     *
     * @return A map of custom metadata map entries that will be added to the custom metadata stored in the
     * {@link software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata} object for the table being mapped.
     * Your extension should know what to do with these custom metadata entries.
     */
    protected abstract Map<String, Object> customMetadata();

    void setTableMetadata(StaticTableMetadata.Builder tableMetadataBuilder) {
        customMetadata().forEach(tableMetadataBuilder::addCustomMetadataObject);
    }
}
