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
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;

/**
 * Controls how an attribute is written by an update operation. It does not affect PutItem. See {@link UpdateBehavior} for the
 * available values.
 * <p>For a bean or immutable object stored as a named nested attribute:
 * <ul>
 *     <li>{@link IgnoreNullsMode#SCALAR_ONLY} updates its fields separately, so their tags apply. The nested object must
 *     already exist as a map in DynamoDB.</li>
 *     <li>{@link IgnoreNullsMode#MAPS_ONLY} and {@link IgnoreNullsMode#DEFAULT} write the object as one map. A tag on the
 *     top-level object property can apply to that map; tags on its fields are not evaluated separately.</li>
 * </ul>
 * <p>For collections of child objects, tags on fields inside a child are not evaluated:
 * <ul>
 *     <li>{@code List<Child>}: a tag on the list property can apply when the whole list is written.</li>
 *     <li>{@code Map<String, Child>}: a tag on the map property can apply when the whole map is written with
 *     {@code MAPS_ONLY} or {@code DEFAULT}. It does not apply to entries updated with {@code SCALAR_ONLY}; the map and each
 *     entry being updated must already exist as maps.</li>
 *     <li>{@code Set<Child>}: the default converter does not support sets of child objects.</li>
 * </ul>
 * <p>Null top-level attributes are skipped with {@code SCALAR_ONLY} or {@code MAPS_ONLY} and removed with {@code DEFAULT}.
 * A null field inside an object written as one map can be stored as DynamoDB {@code NULL}. A stored {@code NULL} is considered
 * present by {@link UpdateBehavior#WRITE_IF_NOT_EXISTS}.
 * <p>With {@code SCALAR_ONLY}, top-level attribute names and keys in maps being flattened must not contain the reserved marker
 * {@code _NESTED_ATTR_UPDATE_}. Such names cause an {@link IllegalArgumentException} while preparing the update.
 */
@SdkPublicApi
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(BeanTableSchemaAttributeTags.class)
public @interface DynamoDbUpdateBehavior {

    /**
     * Returns the configured update behavior.
     *
     * @return the behavior to apply when this attribute is updated
     */
    UpdateBehavior value();
}
