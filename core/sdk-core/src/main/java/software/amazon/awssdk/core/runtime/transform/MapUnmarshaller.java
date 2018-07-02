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

package software.amazon.awssdk.core.runtime.transform;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;

import com.fasterxml.jackson.core.JsonToken;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class MapUnmarshaller<K, V> implements Unmarshaller<Map<K, V>, JsonUnmarshallerContext> {

    private final Unmarshaller<K, JsonUnmarshallerContext> keyUnmarshaller;
    private final Unmarshaller<V, JsonUnmarshallerContext> valueUnmarshaller;

    public MapUnmarshaller(Unmarshaller<K, JsonUnmarshallerContext> keyUnmarshaller,
                           Unmarshaller<V, JsonUnmarshallerContext> valueUnmarshaller) {
        this.keyUnmarshaller = keyUnmarshaller;
        this.valueUnmarshaller = valueUnmarshaller;
    }

    public Map<K, V> unmarshall(JsonUnmarshallerContext context) throws Exception {
        Map<K, V> map = new HashMap<>();
        int originalDepth = context.getCurrentDepth();

        if (context.getCurrentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        while (true) {
            JsonToken token = context.nextToken();
            if (token == null) {
                return map;
            }

            if (token == FIELD_NAME) {
                K k = keyUnmarshaller.unmarshall(context);
                context.nextToken();
                V v = valueUnmarshaller.unmarshall(context);
                map.put(k, v);
            } else if (token == END_ARRAY || token == END_OBJECT) {
                if (context.getCurrentDepth() <= originalDepth) {
                    return map;
                }
            }
        }
    }

}
