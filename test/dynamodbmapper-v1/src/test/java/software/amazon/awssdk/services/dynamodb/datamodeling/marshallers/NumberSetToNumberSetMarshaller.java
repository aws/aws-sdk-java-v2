/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.NumberSetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals sets of Java {@code Number}s into DynamoDB
 * NumberSets.
 */
public class NumberSetToNumberSetMarshaller
        implements NumberSetAttributeMarshaller {

    private static final NumberSetToNumberSetMarshaller INSTANCE =
            new NumberSetToNumberSetMarshaller();

    private NumberSetToNumberSetMarshaller() {
    }

    public static NumberSetToNumberSetMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<? extends Number> numbers = (Set<? extends Number>) obj;
        List<String> numberAttributes = new ArrayList<String>(numbers.size());

        for (Number n : numbers) {
            numberAttributes.add(n.toString());
        }

        return AttributeValue.builder().ns(numberAttributes).build();
    }
}
