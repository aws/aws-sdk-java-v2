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

package software.amazon.awssdk.utils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class AttributeMapTest {

    private static final AttributeMap.Key<String> STRING_KEY = new AttributeMap.Key<String>(String.class) {
    };

    private static final AttributeMap.Key<Integer> INTEGER_KEY = new AttributeMap.Key<Integer>(Integer.class) {
    };

    @Test
    public void copyCreatesNewOptionsObject() {
        AttributeMap orig = AttributeMap.builder()
                                        .put(STRING_KEY, "foo")
                                        .build();
        assertTrue(orig != orig.copy());
        assertThat(orig).isEqualTo(orig.copy());
        assertThat(orig.get(STRING_KEY)).isEqualTo(orig.copy().get(STRING_KEY));
    }

    @Test
    public void mergeTreatsThisObjectWithHigherPrecedence() {
        AttributeMap orig = AttributeMap.builder()
                                        .put(STRING_KEY, "foo")
                                        .build();
        AttributeMap merged = orig.merge(AttributeMap.builder()
                                                     .put(STRING_KEY, "bar")
                                                     .put(INTEGER_KEY, 42)
                                                     .build());
        assertThat(merged.containsKey(STRING_KEY)).isTrue();
        assertThat(merged.get(STRING_KEY)).isEqualTo("foo");
        // Integer key is not in 'this' object so it should be merged in from the lower precedence
        assertThat(merged.get(INTEGER_KEY)).isEqualTo(42);
    }

    /**
     * Options are optional.
     */
    @Test
    public void mergeWithOptionNotPresentInBoth_DoesNotThrow() {
        AttributeMap orig = AttributeMap.builder()
                                        .put(STRING_KEY, "foo")
                                        .build();
        AttributeMap merged = orig.merge(AttributeMap.builder()
                                                     .put(STRING_KEY, "bar")
                                                     .build());
        assertThat(merged.get(INTEGER_KEY)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAll_ThrowsRuntimeExceptionWhenTypesMismatched() {
        Map<AttributeMap.Key<?>, Object> attributes = new HashMap<>();
        attributes.put(STRING_KEY, 42);
        AttributeMap.builder()
                    .putAll(attributes)
                    .build();
    }

}