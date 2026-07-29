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

package software.amazon.awssdk.codegen.poet.rules2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            assertEquals(123, index);
        });
        assertTrue(tokenizer.atEof());
    }

    @Test
    public void recognizesDirectIndexedExpression() {
        Tokenizer tokenizer = new Tokenizer("[123]");
        assertTrue(tokenizer.isDirectIndexedAccess());
        tokenizer.consumeDirectIndexed(i -> assertEquals(123, i));
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

    @Test
    public void recognizesDirectNegativeIndexedExpression() {
        Tokenizer tokenizer = new Tokenizer("[-2]");
        assertTrue(tokenizer.isDirectNegativeIndexedAccess());
        tokenizer.consumeDirectNegativeIndexed(i -> assertEquals(-2, i));
        assertTrue(tokenizer.atEof());
    }

    @Test
    public void recognizesNegativeIndexedExpression() {
        Tokenizer tokenizer = new Tokenizer("resourceId[-1]");
        assertTrue(tokenizer.isNegativeIndexedAccess());
        tokenizer.consumeNegativeIndexed((name, index) -> {
            assertEquals("resourceId", name);
            assertEquals(-1, index);
        });
        assertTrue(tokenizer.atEof());
    }

    @Test
    public void negativeIndexIsNotConfusedWithPositiveIndex() {
        assertTrue(new Tokenizer("[2]").isDirectIndexedAccess());
        assertFalse(new Tokenizer("[2]").isDirectNegativeIndexedAccess());
        assertTrue(new Tokenizer("[-2]").isDirectNegativeIndexedAccess());
        assertFalse(new Tokenizer("[-2]").isDirectIndexedAccess());
    }

    /**
     * A hyphen is only a distinct token when it opens a negative index. Everywhere else it is an ordinary string
     * character, otherwise literals such as "s3-fips" would be split into extra concatenation terms by
     * {@code ExpressionParser.parseStringConcat}.
     */
    @Test
    public void hyphenOutsideIndexRemainsPartOfString() {
        // Hyphen followed by a digit, but not inside an index - must stay in the string.
        Tokenizer tokenizer = new Tokenizer("{Region}-1a");
        assertTrue(tokenizer.isReference());
        tokenizer.consumeReferenceAccess(n -> assertEquals("Region", n));
        assertEquals("-1a", tokenizer.next().value());
        assertTrue(tokenizer.atEof());
    }

    @Test
    public void hyphenatedHostLabelIsSingleToken() {
        Tokenizer tokenizer = new Tokenizer("{Region}s3-fips-2.example");
        assertTrue(tokenizer.isReference());
        tokenizer.consumeReferenceAccess(n -> assertEquals("Region", n));
        assertEquals("s3-fips-2.example", tokenizer.next().value());
        assertTrue(tokenizer.atEof());
    }
}