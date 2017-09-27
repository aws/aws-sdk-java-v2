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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class PairTest {

    @Test
    public void equalsMethodWorksAsExpected() {
        Pair foo = Pair.of("Foo", 50);
        assertThat(foo).isEqualTo(Pair.of("Foo", 50));
        assertThat(foo).isNotEqualTo(Pair.of("Foo-bar", 50));
    }

    @Test
    public void canBeUseAsMapKey() {
        Map<Pair<String, Integer>, String> map = new HashMap<>();

        map.put(Pair.of("Hello", 100), "World");

        assertThat(map.get(Pair.of("Hello", 100))).isEqualTo("World");
    }

    @Test
    public void prettyToString() {
        assertThat(Pair.of("Hello", "World").toString()).isEqualTo("Pair(left=Hello, right=World)");
    }

}