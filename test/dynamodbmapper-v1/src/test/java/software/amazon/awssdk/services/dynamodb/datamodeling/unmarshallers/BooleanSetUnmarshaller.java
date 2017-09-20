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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentUnmarshaller;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A special unmarshaller for Set&lt;Boolean>, which the V1 schema stores as
 * an NS using 0/1 for true/false. In the V2 schema these fall through to
 * the {@code ObjectSetToListMarshaller} which stores them as an L or BOOLs.
 */
public class BooleanSetUnmarshaller implements ArgumentUnmarshaller {

    private static final BooleanSetUnmarshaller INSTANCE =
            new BooleanSetUnmarshaller();

    private BooleanSetUnmarshaller() {
    }

    public static BooleanSetUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public void typeCheck(AttributeValue value, Method setter) {
        if (value.ns() == null && value.l() == null) {
            throw new DynamoDbMappingException(
                    "Expected either L or NS in value " + value
                    + " when invoking " + setter);
        }
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        if (value.l() != null) {
            return unmarshallList(value.l());
        } else {
            return unmarshallNs(value.ns());
        }
    }

    private Set<Boolean> unmarshallList(List<AttributeValue> values) {
        Set<Boolean> result = new HashSet<Boolean>();

        for (AttributeValue value : values) {
            Boolean bool;
            if (Boolean.TRUE.equals(value.nul())) {
                bool = null;
            } else {
                bool = value.bool();
                if (bool == null) {
                    throw new DynamoDbMappingException(
                            value + " is not a boolean");
                }
            }

            if (!result.add(bool)) {
                throw new DynamoDbMappingException(
                        "Duplicate value (" + bool + ") found in "
                        + values);
            }
        }

        return result;
    }

    private Set<Boolean> unmarshallNs(List<String> values) {
        Set<Boolean> result = new HashSet<Boolean>();

        for (String s : values) {
            if ("1".equals(s)) {
                result.add(Boolean.TRUE);
            } else if ("0".equals(s)) {
                result.add(Boolean.FALSE);
            } else {
                throw new IllegalArgumentException(
                        "Expected '1' or '0' for boolean value, was " + s);
            }
        }

        return result;
    }
}
