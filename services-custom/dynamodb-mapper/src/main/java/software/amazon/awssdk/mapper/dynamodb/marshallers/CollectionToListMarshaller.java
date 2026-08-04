/*
 * Copyright 2014-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.mapper.dynamodb.marshallers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller;
import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.ListAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class CollectionToListMarshaller implements ListAttributeMarshaller {

    private static final CollectionToListMarshaller INSTANCE =
            new CollectionToListMarshaller();

    public static CollectionToListMarshaller instance() {
        return INSTANCE;
    }


    private final ArgumentMarshaller memberMarshaller;

    private CollectionToListMarshaller() {
        this(null);
    }

    public CollectionToListMarshaller(ArgumentMarshaller memberMarshaller) {
        this.memberMarshaller = memberMarshaller;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        if (memberMarshaller == null) {
            throw new IllegalStateException(
                    "No member marshaller configured!");
        }

        Collection<?> objects = (Collection<?>) obj;
        List<AttributeValue> values =
                new ArrayList<AttributeValue>(objects.size());

        for (Object o : objects) {
            AttributeValue value;
            if (o == null) {
                value = AttributeValue.builder().nul(true).build();
            } else {
                value = memberMarshaller.marshall(o);
            }

            values.add(value);
        }

        return AttributeValue.builder().l(values).build();
    }

    public ArgumentMarshaller getMemberMarshaller() {
        return memberMarshaller;
    }
}
