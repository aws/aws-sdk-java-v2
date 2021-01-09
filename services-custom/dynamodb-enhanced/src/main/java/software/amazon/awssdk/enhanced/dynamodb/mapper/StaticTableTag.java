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

/**
 * Interface for a tag that can be applied to any {@link StaticTableSchema}. When the table schema is instantiated,
 * the table metadata stored on the schema will be updated by calling the {@link #modifyMetadata()} method for every tag
 * associated with the table.
 */
@SdkPublicApi
public interface StaticTableTag {
    /**
     * A function that modifies an existing {@link StaticTableSchema.Builder} when this tag is applied to a table. This
     * will be used by the {@link StaticTableSchema} to capture all the metadata associated with tags that have been
     * applied to the table.
     *
     * @return a consumer that modifies an existing {@link StaticTableSchema.Builder}.
     */
    Consumer<StaticTableMetadata.Builder> modifyMetadata();
}
