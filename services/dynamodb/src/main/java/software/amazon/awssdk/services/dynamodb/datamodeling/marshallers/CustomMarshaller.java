/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling.marshallers;

import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that delegates to an instance of a
 * {@code DynamoDBMarshalling}-derived custom marshaler.
 */
public class CustomMarshaller implements StringAttributeMarshaller {

    private final Class<? extends DynamoDbMarshaller<?>> marshallerClass;

    public CustomMarshaller(
            Class<? extends DynamoDbMarshaller<?>> marshallerClass) {

        this.marshallerClass = marshallerClass;
    }

    @SuppressWarnings("unchecked")
    private static DynamoDbMarshaller<Object> createMarshaller(Class<?> clazz) {
        try {

            return (DynamoDbMarshaller<Object>) clazz.newInstance();

        } catch (InstantiationException e) {
            throw new DynamoDbMappingException(
                    "Failed to instantiate custom marshaler for class " + clazz,
                    e);

        } catch (IllegalAccessException e) {
            throw new DynamoDbMappingException(
                    "Failed to instantiate custom marshaler for class " + clazz,
                    e);
        }
    }

    @Override
    public AttributeValue marshall(Object obj) {

        // TODO: Would be nice to cache this object, but not sure if we can
        // do that now without a breaking change; user's marshalers might
        // not all be thread-safe.

        DynamoDbMarshaller<Object> marshaler =
                createMarshaller(marshallerClass);

        String stringValue = marshaler.marshall(obj);

        if (stringValue == null) {
            return null;
        } else {
            return AttributeValue.builder().s(stringValue).build();
        }
    }
}
