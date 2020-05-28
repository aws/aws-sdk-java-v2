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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * Validate the functionality of {@link ToString}.
 */
public class ToStringTest {
    @Test
    public void createIsCorrect() {
        assertThat(ToString.create("Foo")).isEqualTo("Foo()");
    }

    @Test
    public void buildIsCorrect() {
        assertThat(ToString.builder("Foo").build()).isEqualTo("Foo()");
        assertThat(ToString.builder("Foo").add("a", "a").build()).isEqualTo("Foo(a=a)");
        assertThat(ToString.builder("Foo").add("a", "a").add("b", "b").build()).isEqualTo("Foo(a=a, b=b)");
        assertThat(ToString.builder("Foo").add("a", 1).build()).isEqualTo("Foo(a=1)");
        assertThat(ToString.builder("Foo").add("a", 'a').build()).isEqualTo("Foo(a=a)");
        assertThat(ToString.builder("Foo").add("a", Arrays.asList("a", "b")).build()).isEqualTo("Foo(a=[a, b])");
        assertThat(ToString.builder("Foo").add("a", new String[] {"a", "b"}).build()).isEqualTo("Foo(a=[a, b])");
        assertThat(ToString.builder("Foo").add("a", Collections.singletonMap("a", "b")).build()).isEqualTo("Foo(a={a=b})");
    }
}
