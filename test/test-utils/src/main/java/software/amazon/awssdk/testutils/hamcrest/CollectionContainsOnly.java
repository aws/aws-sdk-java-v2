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

package software.amazon.awssdk.testutils.hamcrest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class CollectionContainsOnly<T> extends TypeSafeMatcher<Collection<T>> {
    private final List<Matcher<? extends T>> matchers;

    private CollectionContainsOnly(List<Matcher<? extends T>> matchers) {
        this.matchers = matchers;
    }

    static <T> TypeSafeMatcher<Collection<T>> containsOnly(List<Matcher<? extends T>> matchers) {
        return new CollectionContainsOnly<T>(matchers);
    }

    @Override
    protected boolean matchesSafely(Collection<T> actualItems) {
        final List<Matcher<? extends T>> copyOfExpected = new ArrayList<>(matchers);
        for (T item : actualItems) {
            boolean match = false;
            for (Matcher<? extends T> m : copyOfExpected) {
                if (m.matches(item)) {
                    copyOfExpected.remove(m);
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return copyOfExpected.isEmpty();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("collection containing ").appendList("[", ", ", "]", matchers);
    }
}
