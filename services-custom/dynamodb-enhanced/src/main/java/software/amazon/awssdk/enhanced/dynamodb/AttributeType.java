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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Attribute type helps to convert attribute values from and to their {@link AttributeValue} representation.
 * @param <T> the type of the attribute.
 */
@SdkPublicApi
public interface AttributeType<T> {

    /**
     * Converts an object representing value of a attribute into {@link AttributeValue}.
     *
     * @param object an object to be converted.
     * @return {@link AttributeValue} representation of the object.
     */
    AttributeValue objectToAttributeValue(T object);

    /**
     * Converts an {@link AttributeValue} to the value expected by the attribute.
     * @param attributeValue  {@link AttributeValue} to be converted.
     * @return the object representation of the {@link AttributeValue}.
     */
    T attributeValueToObject(AttributeValue attributeValue);

    /**
     * Returns the {@link AttributeValueType} representation of the Java class of the attribute.
     * @return the {@link AttributeValueType} representation of the Java class of the attribute.
     */
    AttributeValueType attributeValueType();
}
