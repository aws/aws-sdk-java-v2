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

package software.amazon.awssdk.services.dynamodb.document;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.document.internal.InternalUtils;

public class ItemTestUtils {
    /**
     * Used for testing purposes.
     */
    public static boolean equalsItem(Item itemFrom, Item itemTo) {
        return equalsSimpleValue(itemFrom.asMap(), itemTo.asMap());
    }

    /**
     * Used for testing purposes.
     */
    @SuppressWarnings("unchecked")
    public static boolean equalsSimpleValue(Object v0, Object v1) {
        if (v0 == null || v1 == null) {
            return v0 == null && v1 == null;
        }
        // Byte buffer or byte array
        if (v0 instanceof ByteBuffer) {
            return equalsByteBuffer((ByteBuffer) v0, v1);
        } else if (v1 instanceof ByteBuffer) {
            return equalsByteBuffer((ByteBuffer) v1, v0);
        } else if (v0 instanceof byte[]) {
            return equalsByteArray((byte[]) v0, v1);
        } else if (v1 instanceof byte[]) {
            return equalsByteArray((byte[]) v1, v0);
        }
        // Number
        if (v0 instanceof Number && v1 instanceof Number) {
            String s0 = InternalUtils.valToString(v0);
            String s1 = InternalUtils.valToString(v1);
            return s0.equals(s1);
        }
        // Map
        if (v0 instanceof Map && v1 instanceof Map) {
            Map<String, Object> map0 = (Map<String, Object>) v0;
            Map<String, Object> map1 = (Map<String, Object>) v1;

            if (map0.size() != map1.size()) {
                return false;
            }

            for (Entry<String, Object> e : map0.entrySet()) {
                if (!equalsSimpleValue(
                        e.getValue(),
                        map1.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        if (v0 instanceof List && v1 instanceof List) {
            List<Object> map0 = (List<Object>) v0;
            List<Object> map1 = (List<Object>) v1;

            if (map0.size() != map1.size()) {
                return false;
            }
            for (int i = 0; i < map0.size(); i++) {
                if (!equalsSimpleValue(map0.get(i), map1.get(i))) {
                    return false;
                }
            }
            return true;
        }
        // Set
        // Currently this works only if both set have the elements in the same
        // iteration order.  Can we do better ?
        if (v0 instanceof Set && v1 instanceof Set) {
            Set<Object> set0 = (Set<Object>) v0;
            Set<Object> set1 = (Set<Object>) v1;

            if (set0.size() != set1.size()) {
                return false;
            }

            for (Object element0 : set0) {
                boolean matchFound = false;

                for (Object element1 : set1) {
                    if (equalsSimpleValue(element0, element1)) {
                        matchFound = true;
                        break;
                    }
                }

                if (!matchFound) {
                    return false;
                }
            }

            return true;
        }

        return v0.equals(v1);
    }

    /**
     * Used for testing purposes.
     */
    private static boolean equalsByteBuffer(ByteBuffer fromByteBuffer, Object o) {
        byte[] from = fromByteBuffer.array();
        return equalsByteArray(from, o);
    }

    /**
     * Used for testing purposes.
     */
    private static boolean equalsByteArray(byte[] from, Object o) {
        if (o instanceof ByteBuffer) {
            byte[] to = ((ByteBuffer) o).array();
            return Arrays.equals(from, to);
        } else {
            return (o instanceof byte[])
                   && Arrays.equals(from, ((byte[]) o));
        }
    }
}
