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

package software.amazon.awssdk.mapper.dynamodb.internal.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller;
import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.MapAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class MapToMapMarshaller implements MapAttributeMarshaller {

    private static final MapToMapMarshaller INSTANCE =
            new MapToMapMarshaller();

    public static MapToMapMarshaller instance() {
        return INSTANCE;
    }


    private final ArgumentMarshaller memberMarshaller;

    private MapToMapMarshaller() {
        memberMarshaller = null;
    }

    public MapToMapMarshaller(ArgumentMarshaller memberMarshaller) {
        if (memberMarshaller == null) {
            throw new NullPointerException("memberMarshaller");
        }
        this.memberMarshaller = memberMarshaller;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        if (memberMarshaller == null) {
            throw new IllegalStateException(
                    "No member marshaller configured!");
        }

        @SuppressWarnings("unchecked")
        Map<String, ?> map = (Map<String, ?>) obj;
        Map<String, AttributeValue> values =
                new HashMap<String, AttributeValue>();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            AttributeValue value;
            if (entry.getValue() == null) {
                value = AttributeValue.builder().nul(true).build();
            } else {
                value = memberMarshaller.marshall(entry.getValue());
            }

            values.put(entry.getKey(), value);
        }

        return AttributeValue.builder().m(values).build();
    }

    public ArgumentMarshaller getMemberMarshaller() {
        return memberMarshaller;
    }
}
