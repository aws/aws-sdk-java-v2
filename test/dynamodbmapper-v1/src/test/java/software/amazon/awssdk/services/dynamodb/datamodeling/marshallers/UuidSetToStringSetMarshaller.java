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
import java.util.UUID;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.StringSetAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals sets of Java {@code Object} objects into
 * DynamoDB StringSets.
 */
public class UuidSetToStringSetMarshaller
        implements StringSetAttributeMarshaller {

    private static final UuidSetToStringSetMarshaller INSTANCE =
            new UuidSetToStringSetMarshaller();

    private UuidSetToStringSetMarshaller() {
    }

    public static UuidSetToStringSetMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<UUID> uuids = (Set<UUID>) obj;

        List<String> strings = new ArrayList<String>(uuids.size());
        for (UUID uuid : uuids) {
            strings.add(uuid.toString());
        }

        return AttributeValue.builder().ss(strings).build();
    }
}
