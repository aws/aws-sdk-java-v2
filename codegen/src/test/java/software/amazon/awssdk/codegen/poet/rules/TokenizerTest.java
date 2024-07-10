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

package software.amazon.awssdk.codegen.poet.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenizerTest {

    @Test
    public void recognizesReferenceExpression() {
        Tokenizer tokenizer = new Tokenizer("{foobar}");
        assertTrue(tokenizer.isReference());
        tokenizer.consumeReferenceAccess((n) -> assertEquals("foobar", n));
        assertTrue(tokenizer.atEof());
    }

    @Test
    public void recognizesIndexedExpression() {
        Tokenizer tokenizer = new Tokenizer("foobar[123]");
        assertTrue(tokenizer.isIndexedAccess());
        tokenizer.consumeIndexed((name, index) -> {
            assertEquals("foobar", name);
            assertEquals(index, 123);
        });
        assertTrue(tokenizer.atEof());
    }

    @Test
    public void recognizesNamedAccessExpression() {
        Tokenizer tokenizer = new Tokenizer("{url#authority}");
        assertTrue(tokenizer.isNamedAccess());
        tokenizer.consumeNamedAccess((reference, name) -> {
            assertEquals("url", reference);
            assertEquals("authority", name);
        });
        assertTrue(tokenizer.atEof());
    }
}