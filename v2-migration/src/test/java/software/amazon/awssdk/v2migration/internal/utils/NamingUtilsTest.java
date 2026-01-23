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

package software.amazon.awssdk.v2migration.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class NamingUtilsTest {
    @Test
    public void removeSet_startsWithSet_removed() {
        assertThat(NamingUtils.removeSet("setFoo")).isEqualTo("foo");
    }

    @Test
    public void removeSet_containsTwoInstances_removesOnlyFirst() {
        assertThat(NamingUtils.removeSet("setSet")).isEqualTo("set");
    }

    @Test
    public void removeSet_doesNotStartWithSet_unchanged() {
        String n = "fooSet";
        assertThat(NamingUtils.removeSet(n)).isEqualTo(n);
    }

    @Test
    public void removeWith_startsWithSet_removed() {
        assertThat(NamingUtils.removeWith("withFoo")).isEqualTo("foo");
    }

    @Test
    public void removeWith_containsTwoInstances_removesOnlyFirst() {
        assertThat(NamingUtils.removeWith("withWith")).isEqualTo("with");
    }

    @Test
    public void removeWith_doesNotStartWithSet_unchanged() {
        String n = "fooWith";
        assertThat(NamingUtils.removeWith(n)).isEqualTo(n);
    }

    @Test
    public void isSetter_startsWithSet_returnsTrue() {
        assertThat(NamingUtils.isSetter("setFoo")).isTrue();
    }

    @Test
    public void isSetter_doesNotStartWithSet_returnsFalse() {
        assertThat(NamingUtils.isSetter("foo")).isFalse();
    }

    @Test
    public void isWither_startsWithWith_returnsTrue() {
        assertThat(NamingUtils.isWither("withFoo")).isTrue();
    }

    @Test
    public void isWither_doesNotStartWithWith_returnsFalse() {
        assertThat(NamingUtils.isWither("foo")).isFalse();
    }

    @Test
    public void removeSet_null_returnsValue() {
        assertThat(NamingUtils.removeSet(null)).isNull();
    }

    @Test
    public void removeWith_null_returnsValue() {
        assertThat(NamingUtils.removeWith(null)).isNull();
    }

    @Test
    public void removeSet_empty_returnsValue() {
        assertThat(NamingUtils.removeSet("")).isEqualTo("");
    }

    @Test
    public void removeWith_empty_returnsValue() {
        assertThat(NamingUtils.removeWith("")).isEqualTo("");
    }

    @Test
    public void isGetter_startsWithGetter_returnsTrue() {
        assertThat(NamingUtils.isGetter("getFoo")).isTrue();
    }

    @Test
    public void isGetter_equalsToGet_returnsFalse() {
        assertThat(NamingUtils.isGetter("get")).isFalse();
    }

    @Test
    public void isGetter_emptyOrNull_returnsFalse() {
        assertThat(NamingUtils.isGetter("")).isFalse();
        assertThat(NamingUtils.isGetter(null)).isFalse();
    }
}
