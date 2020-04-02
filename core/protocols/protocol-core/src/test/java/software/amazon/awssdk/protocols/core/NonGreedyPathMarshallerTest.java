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

public class NonGreedyPathMarshallerTest {

    private final PathMarshaller marshaller = PathMarshaller.NON_GREEDY;

    @Test(expected = NullPointerException.class)
    public void nullPathValue_ThrowsException() {
        marshaller.marshall("/foo/{pathParam}", "pathParam", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPathValue_ThrowsException() {
        marshaller.marshall("/foo/{pathParam}", "pathParam", "");
    }

    @Test
    public void nonEmptyPathValue_ReplacesPlaceholder() {
        assertEquals("/foo/nonEmpty", marshaller.marshall("/foo/{pathParam}", "pathParam", "nonEmpty"));
    }

    @Test
    public void pathValueWithSlashes_UrlEncodedWhenReplaced() {
        assertEquals("/foo/has%2Fslash", marshaller.marshall("/foo/{pathParam}", "pathParam", "has/slash"));
    }

}
