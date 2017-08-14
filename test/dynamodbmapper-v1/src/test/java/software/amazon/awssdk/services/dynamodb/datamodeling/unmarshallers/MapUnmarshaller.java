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
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentUnmarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class MapUnmarshaller extends MUnmarshaller {

    private static final MapUnmarshaller INSTANCE = new MapUnmarshaller();
    private final ArgumentUnmarshaller memberUnmarshaller;

    private MapUnmarshaller() {
        memberUnmarshaller = null;
    }

    public MapUnmarshaller(ArgumentUnmarshaller memberUnmarshaller) {
        if (memberUnmarshaller == null) {
            throw new NullPointerException("memberUnmarshaller");
        }
        this.memberUnmarshaller = memberUnmarshaller;
    }

    public static MapUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) throws ParseException {
        Map<String, AttributeValue> map = value.m();
        Map<String, Object> result = new HashMap<String, Object>();

        for (Map.Entry<String, AttributeValue> entry : map.entrySet()) {
            memberUnmarshaller.typeCheck(entry.getValue(), null);
            result.put(entry.getKey(),
                       memberUnmarshaller.unmarshall(entry.getValue()));
        }

        return result;
    }
}
