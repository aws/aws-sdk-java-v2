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

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.MapAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.datamodeling.ItemConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ObjectToMapMarshaller implements MapAttributeMarshaller {

    private static final ObjectToMapMarshaller INSTANCE =
            new ObjectToMapMarshaller();
    private final ItemConverter converter;

    private ObjectToMapMarshaller() {
        converter = null;
    }

    public ObjectToMapMarshaller(ItemConverter converter) {
        if (converter == null) {
            throw new NullPointerException("converter");
        }
        this.converter = converter;
    }

    public static ObjectToMapMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        Map<String, AttributeValue> values = converter.convert(obj);
        return AttributeValue.builder().m(values).build();
    }
}
