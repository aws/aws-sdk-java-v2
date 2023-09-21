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

package software.amazon.awssdk.enhanced.dynamodb.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags;

/**
 * Denotes an optional sort key for a global or local secondary index.
 *
 * <p>You must also specify at least one index name. For global secondary indices, this must match an index name specified in
 * a {@link DynamoDbSecondaryPartitionKey}. Any index names specified that do not have an associated
 * {@link DynamoDbSecondaryPartitionKey} are treated as local secondary indexes.
 *
 * <p>The index name will be used if a table is created from this bean. For data-oriented operations like reads and writes, this
 * name does not need to match the service-side name of the index.
 */
@SdkPublicApi
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(BeanTableSchemaAttributeTags.class)
public @interface DynamoDbSecondarySortKey {
    /**
     * The names of one or more local or global secondary indices that this sort key should participate in.
     *
     * <p>For global secondary indices, this must match an index name specified in a {@link DynamoDbSecondaryPartitionKey}. Any
     * index names specified that do not have an associated {@link DynamoDbSecondaryPartitionKey} are treated as local
     * secondary indexes.
     */
    String[] indexNames();
}
