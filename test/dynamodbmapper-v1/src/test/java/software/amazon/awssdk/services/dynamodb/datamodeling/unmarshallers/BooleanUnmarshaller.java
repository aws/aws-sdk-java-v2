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

package software.amazon.awssdk.services.dynamodb.datamodeling.unmarshallers;

import java.lang.reflect.Method;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentUnmarshaller;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMappingException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals DynamoDB Bools (or Numbers) into Java
 * {@code Boolean}s. Numbers are handled for backwards compatibility with
 * versions of the mapper written before the DynamoDB native Boolean type
 * was added, which stored Java {@code Boolean}s as either the Number 0 (false)
 * or 1 (true).
 */
public class BooleanUnmarshaller implements ArgumentUnmarshaller {

    private static final BooleanUnmarshaller INSTANCE =
            new BooleanUnmarshaller();

    private BooleanUnmarshaller() {
    }

    public static BooleanUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public void typeCheck(AttributeValue value, Method setter) {
        if (value.n() == null && value.bool() == null) {
            throw new DynamoDbMappingException(
                    "Expected either N or BOOL in value " + value
                    + " when invoking " + setter);
        }
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        if (value.bool() != null) {
            return value.bool();
        }
        if ("1".equals(value.n())) {
            return Boolean.TRUE;
        }
        if ("0".equals(value.n())) {
            return Boolean.FALSE;
        }

        throw new IllegalArgumentException(
                "Expected '1', '0', or BOOL value for boolean value, was "
                + value);
    }
}
