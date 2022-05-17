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

package software.amazon.awssdk.enhanced.dynamodb.model;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class PageTest {
    @Test
    public void nullToStringIsCorrect() {
        assertThat(Page.create(null)).hasToString("Page()");
    }

    @Test
    public void emptyToStringIsCorrect() {
        assertThat(Page.create(emptyList())).hasToString("Page(items=[])");
    }

    @Test
    public void fullToStringIsCorrect() {
        assertThat(Page.create(asList("foo", "bar"), singletonMap("foo", AttributeValue.builder().s("bar").build())))
            .hasToString("Page(lastEvaluatedKey={foo=AttributeValue(S=bar)}, items=[foo, bar])");
    }
}