/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.mapper.dynamodb.internal.unmarshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import software.amazon.awssdk.mapper.dynamodb.ArgumentUnmarshaller;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMappingException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class ObjectSetUnmarshaller extends LUnmarshaller {

    private static final ObjectSetUnmarshaller INSTANCE =
            new ObjectSetUnmarshaller();

    public static ObjectSetUnmarshaller instance() {
        return INSTANCE;
    }

    private final ArgumentUnmarshaller memberUnmarshaller;

    private ObjectSetUnmarshaller() {
        memberUnmarshaller = null;
    }

    public ObjectSetUnmarshaller(ArgumentUnmarshaller memberUnmarshaller) {
        if (memberUnmarshaller == null) {
            throw new NullPointerException("memberUnmarshaller");
        }
        this.memberUnmarshaller = memberUnmarshaller;
    }

    @Override
    public Object unmarshall(AttributeValue value) throws ParseException {
        List<AttributeValue> values = value.l();

        // As in the LinkedHashSet(Collection) constructor.
        int size = Math.max(values.size() * 2, 11);
        Set<Object> objects =  new LinkedHashSet<Object>(size);

        for (AttributeValue v : values) {
            memberUnmarshaller.typeCheck(v, null);
            Object o = memberUnmarshaller.unmarshall(v);
            if (!objects.add(o)) {
                throw new DynamoDBMappingException(
                        "Duplicate value (" + o + ") found in " + values);
            }
        }

        return objects;
    }
}
