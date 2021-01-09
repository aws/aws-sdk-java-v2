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

package software.amazon.awssdk.protocols.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GreedyPathMarshallerTest {

    private final PathMarshaller marshaller = PathMarshaller.GREEDY;

    @Test(expected = NullPointerException.class)
    public void nullPathValue_ThrowsException() {
        marshaller.marshall("/foo/{greedyParam+}", "greedyParam", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPathValue_ThrowsException() {
        marshaller.marshall("/foo/{greedyParam+}", "greedyParam", "");
    }

    @Test
    public void nonEmptyPathValue_ReplacesPlaceholder() {
        assertEquals("/foo/nonEmpty", marshaller.marshall("/foo/{greedyParam+}", "greedyParam", "nonEmpty"));
    }

    @Test
    public void pathValueWithSlashes_NotUrlEncodedWhenReplaced() {
        assertEquals("/foo/my/greedy/value", marshaller.marshall("/foo/{greedyParam+}", "greedyParam", "my/greedy/value"));
    }

    @Test
    public void pathValueWithLeadingSlash_TrimmedBeforeReplaced() {
        assertEquals("/foo/my/greedy/value", marshaller.marshall("/foo/{greedyParam+}", "greedyParam", "/my/greedy/value"));
    }

}
