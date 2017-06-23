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

package software.amazon.awssdk.test.util.hamcrest;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CollectionContainsOnlyInOrder<T> extends TypeSafeMatcher<Collection<T>> {

    private final List<Matcher<? extends T>> matchers;

    private CollectionContainsOnlyInOrder(List<Matcher<? extends T>> matchers) {
        this.matchers = matchers;
    }

    static <T> TypeSafeMatcher<Collection<T>> containsOnlyInOrder(List<Matcher<? extends T>> matchers) {
        return new CollectionContainsOnlyInOrder<T>(matchers);
    }

    @Override
    protected boolean matchesSafely(Collection<T> actualItems) {
        final Queue<Matcher<? extends T>> copyOfMatchers = new LinkedList<>(matchers);
        for (T item : actualItems) {
            if (copyOfMatchers.isEmpty() || !copyOfMatchers.remove().matches(item)) {
                return false;
            }
        }
        return copyOfMatchers.isEmpty();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("collection containing ").appendList("[", ", ", "]", matchers).appendText(" in order ");
    }
}
