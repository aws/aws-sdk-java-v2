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

package software.amazon.awssdk.services.dynamodb.datamodeling.unmarshallers;

import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that delegates to an instance of a
 * {@code DynamoDBMarshaller}-derived custom marshaler.
 */
public class CustomUnmarshaller extends SUnmarshaller {

    private final Class<?> targetClass;
    private final Class<? extends DynamoDbMarshaller<?>> unmarshallerClass;

    public CustomUnmarshaller(
            Class<?> targetClass,
            Class<? extends DynamoDbMarshaller<?>> unmarshallerClass) {

        this.targetClass = targetClass;
        this.unmarshallerClass = unmarshallerClass;
    }

    @SuppressWarnings({"rawtypes"})
    private static DynamoDbMarshaller createUnmarshaller(Class<?> clazz) {
        try {

            return (DynamoDbMarshaller) clazz.newInstance();

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object unmarshall(AttributeValue value) {

        // TODO: Would be nice to cache this object, but not sure if we can
        // do that now without a breaking change; user's unmarshallers
        // might not all be thread-safe.

        DynamoDbMarshaller unmarshaller =
                createUnmarshaller(unmarshallerClass);

        return unmarshaller.unmarshall(targetClass, value.s());
    }
}
