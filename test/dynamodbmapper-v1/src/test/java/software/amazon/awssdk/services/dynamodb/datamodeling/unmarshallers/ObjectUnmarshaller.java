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

import java.text.ParseException;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.datamodeling.ItemConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ObjectUnmarshaller extends MUnmarshaller {

    private static final ObjectUnmarshaller INSTANCE = new ObjectUnmarshaller();
    private final ItemConverter converter;
    private final Class<?> clazz;

    private ObjectUnmarshaller() {
        converter = null;
        clazz = null;
    }

    public ObjectUnmarshaller(ItemConverter converter, Class<?> clazz) {
        if (converter == null) {
            throw new NullPointerException("converter");
        }
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }

        this.converter = converter;
        this.clazz = clazz;
    }

    public static ObjectUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) throws ParseException {
        Map<String, AttributeValue> map = value.m();
        return converter.unconvert(clazz, map);
    }
}
