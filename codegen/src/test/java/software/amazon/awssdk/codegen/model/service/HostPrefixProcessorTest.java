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

package software.amazon.awssdk.codegen.model.service;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import org.junit.Test;

public class HostPrefixProcessorTest {

    @Test
    public void staticHostLabel() {
        String hostPrefix = "data-";

        HostPrefixProcessor processor = new HostPrefixProcessor(hostPrefix);
        assertThat(processor.hostWithStringSpecifier(), equalTo("data-"));
        assertThat(processor.c2jNames(), empty());
    }

    @Test
    public void inputShapeLabels() {
        String hostPrefix = "{Bucket}-{AccountId}.";

        HostPrefixProcessor processor = new HostPrefixProcessor(hostPrefix);
        assertThat(processor.hostWithStringSpecifier(), equalTo("%s-%s."));
        assertThat(processor.c2jNames(), contains("Bucket", "AccountId"));
    }

    @Test
    public void emptyCurlyBraces() {
        // Pattern should not match the first set of curly braces as there is no characters between them
        String host = "{}.foo";

        HostPrefixProcessor processor = new HostPrefixProcessor(host);
        assertThat(processor.hostWithStringSpecifier(), equalTo("{}.foo"));
        assertThat(processor.c2jNames(), empty());
    }

    @Test (expected = IllegalArgumentException.class)
    public void emptyHost() {
        new HostPrefixProcessor("");
    }

    @Test (expected = IllegalArgumentException.class)
    public void nullHost() {
        new HostPrefixProcessor(null);
    }
}
