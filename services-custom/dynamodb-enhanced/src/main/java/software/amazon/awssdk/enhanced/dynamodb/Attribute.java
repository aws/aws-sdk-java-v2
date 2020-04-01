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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Represents an attribute of a table model.
 *
 * @param <T> type of the model.
 * @param <R> type type of the attribute.
 */
@SdkPublicApi
public interface Attribute<T, R> {

    /**
     * Returns the name of the attribute.
     *
     * @return the name of the attribute.
     */
    String attributeName();

    /**
     * Returns the {@link AttributeType} of the attribute which helps with converting the values.
     *
     * @return the {@link AttributeType} of the attribute which helps with converting the values.
     */
    AttributeType<R> attributeType();

    /**
     * Returns the function which can be used to obtain the attribute value as {@link AttributeValue} from given object.
     *
     * @return the function which can be used to obtain the attribute value as {@link AttributeValue} from given object.
     */
    Function<T, AttributeValue> attributeGetterMethod();

    /**
     * Returns the consumer which can be used to set the attribute value as {@link AttributeValue} from given object.
     *
     * @return the consumer which can be used to set the attribute value as {@link AttributeValue} from given object.
     */
    BiConsumer<T, AttributeValue> updateItemMethod();

    /**
     * Returns the {@link TableMetadata} of the parent model.
     *
     * @return the {@link TableMetadata} of the parent model.
     */
    TableMetadata tableMetadata();
}
