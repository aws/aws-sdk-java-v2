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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringSetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals sets of Java {@code String}s to DynamoDB
 * StringSets.
 */
public class StringSetToStringSetMarshaller
        implements StringSetAttributeMarshaller {

    private static final StringSetToStringSetMarshaller INSTANCE =
            new StringSetToStringSetMarshaller();

    private StringSetToStringSetMarshaller() {
    }

    public static StringSetToStringSetMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) obj;

        List<String> strings = new ArrayList<String>(set.size());
        for (String s : set) {
            strings.add(s);
        }

        return AttributeValue.builder().ss(strings).build();
    }
}
