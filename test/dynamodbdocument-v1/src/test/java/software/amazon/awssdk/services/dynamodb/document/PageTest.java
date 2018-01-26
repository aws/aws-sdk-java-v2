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

package software.amazon.awssdk.services.dynamodb.document;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PageTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNull_content() {
        new TestPage(null, new Object());
    }

    ;

    @Test(expected = IllegalArgumentException.class)
    public void testNull_result() {
        new TestPage(new ArrayList<String>(), null);
    }

    @Test
    public void test_toString() {
        System.out.println(new TestPage(new ArrayList<String>(), new Object())
                                   .toString());
    }

    private static class TestPage extends Page<String, Object> {
        TestPage(List<String> content, Object result) {
            super(content, result);
        }

        ;

        @Override
        public boolean hasNextPage() {
            return false;
        }

        @Override
        public TestPage nextPage() {
            return null;
        }
    }
}
