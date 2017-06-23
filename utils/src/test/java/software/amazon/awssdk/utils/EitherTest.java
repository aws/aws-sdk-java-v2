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
import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;

public class EitherTest {

    @Test(expected = NullPointerException.class)
    public void leftValueNull_ThrowsException() {
        Either.left(null);
    }

    @Test(expected = NullPointerException.class)
    public void rightValueNull_ThrowsException() {
        Either.right(null);
    }

    @Test
    public void mapWhenLeftValuePresent_OnlyCallsLeftFunction() {
        final Either<String, Integer> either = Either.left("left val");
        final Boolean mapped = either.map(s -> {
                                              assertThat(s).isEqualTo("left val");
                                              return Boolean.TRUE;
                                          },
                                          this::assertNotCalled);
        assertThat(mapped).isTrue();
    }

    @Test
    public void mapWhenRightValuePresent_OnlyCallsRightFunction() {
        final Either<String, Integer> either = Either.right(42);
        final Boolean mapped = either.map(this::assertNotCalled,
                                          i -> {
                                              assertThat(i).isEqualTo(42);
                                              return Boolean.TRUE;
                                          });
        assertThat(mapped).isTrue();
    }

    @Test
    public void mapLeftWhenLeftValuePresent_OnlyMapsLeftValue() {
        final String value = "left val";
        final Either<String, Integer> either = Either.left(value);
        either.mapLeft(String::hashCode)
              .apply(l -> assertThat(l).isEqualTo(value.hashCode()),
                     this::assertNotCalled);
    }

    @Test
    public void mapLeftWhenLeftValueNotPresent_DoesNotMapValue() {
        final Integer value = 42;
        final Either<String, Integer> either = Either.right(value);
        either.mapLeft(this::assertNotCalled)
              .apply(this::assertNotCalled,
                     i -> assertThat(i).isEqualTo(42));
    }

    @Test
    public void mapRightWhenRightValuePresent_OnlyMapsLeftValue() {
        final Integer value = 42;
        final Either<String, Integer> either = Either.right(value);
        either.mapRight(i -> "num=" + i)
              .apply(this::assertNotCalled,
                     v -> assertThat(v).isEqualTo("num=42"));
    }

    @Test
    public void mapRightWhenRightValueNotPresent_DoesNotMapValue() {
        final String value = "left val";
        final Either<String, Integer> either = Either.left(value);
        either.mapRight(this::assertNotCalled)
              .apply(s -> assertThat(s).isEqualTo(value),
                     this::assertNotCalled);
    }

    private <InT, OutT> OutT assertNotCalled(InT in) {
        fail("Mapping function should not have been called");
        return null;
    }
}