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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringSetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A legacy marshaller that marshals sets of arbitrary Java objects into
 * DynamoDB StringSets by using {@link String#valueOf(Object)}. Retained for
 * backwards compatibility in case someone is relying on this, but logs a
 * warning if ever used since we only know how to unmarshal back to Java
 * Strings.
 */
public class ObjectSetToStringSetMarshaller
        implements StringSetAttributeMarshaller {

    private static final Logger log =
            LoggerFactory.getLogger(ObjectSetToStringSetMarshaller.class);

    private static final ObjectSetToStringSetMarshaller INSTANCE =
            new ObjectSetToStringSetMarshaller();

    private ObjectSetToStringSetMarshaller() {
    }

    public static ObjectSetToStringSetMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        Set<?> set = (Set<?>) obj;

        log.warn("Marshaling a set of non-String objects to a DynamoDB "
                 + "StringSet. You won't be able to read these objects back "
                 + "out of DynamoDB unless you REALLY know what you're doing: "
                 + "it's probably a bug. If you DO know what you're doing feel"
                 + "free to ignore this warning, but consider using a custom "
                 + "marshaler for this instead.");

        List<String> strings = new ArrayList<String>(set.size());
        for (Object o : set) {
            strings.add(String.valueOf(o));
        }

        return AttributeValue.builder().ss(strings).build();
    }
}
