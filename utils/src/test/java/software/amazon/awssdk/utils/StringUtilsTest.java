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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for methods of {@link StringUtils}.
 *
 * Adapted from https://github.com/apache/commons-lang.
 */
public class StringUtilsTest {
    private static final String FOO_UNCAP = "foo";
    private static final String FOO_CAP = "Foo";

    private static final String SENTENCE_UNCAP = "foo bar baz";
    private static final String SENTENCE_CAP = "Foo Bar Baz";

    @Test
    public void testUpperCase() {
        assertNull(StringUtils.upperCase(null));
        assertEquals("upperCase(String) failed",
                "FOO TEST THING", StringUtils.upperCase("fOo test THING"));
        assertEquals("upperCase(empty-string) failed",
                "", StringUtils.upperCase(""));
    }

    @Test
    public void testLowerCase() {
        assertNull(StringUtils.lowerCase(null));
        assertEquals("lowerCase(String) failed",
                "foo test thing", StringUtils.lowerCase("fOo test THING"));
        assertEquals("lowerCase(empty-string) failed",
                "", StringUtils.lowerCase(""));
    }

    @Test
    public void testCapitalize() {
        assertNull(StringUtils.capitalize(null));

        assertEquals("capitalize(empty-string) failed",
                "", StringUtils.capitalize(""));
        assertEquals("capitalize(single-char-string) failed",
                "X", StringUtils.capitalize("x"));
        assertEquals("capitalize(String) failed",
                FOO_CAP, StringUtils.capitalize(FOO_CAP));
        assertEquals("capitalize(string) failed",
                FOO_CAP, StringUtils.capitalize(FOO_UNCAP));

        assertEquals("capitalize(String) is not using TitleCase",
                "\u01C8", StringUtils.capitalize("\u01C9"));

        // Javadoc examples
        assertNull(StringUtils.capitalize(null));
        assertEquals("", StringUtils.capitalize(""));
        assertEquals("Cat", StringUtils.capitalize("cat"));
        assertEquals("CAt", StringUtils.capitalize("cAt"));
        assertEquals("'cat'", StringUtils.capitalize("'cat'"));
    }

    @Test
    public void testUnCapitalize() {
        assertNull(StringUtils.uncapitalize(null));

        assertEquals("uncapitalize(String) failed",
                FOO_UNCAP, StringUtils.uncapitalize(FOO_CAP));
        assertEquals("uncapitalize(string) failed",
                FOO_UNCAP, StringUtils.uncapitalize(FOO_UNCAP));
        assertEquals("uncapitalize(empty-string) failed",
                "", StringUtils.uncapitalize(""));
        assertEquals("uncapitalize(single-char-string) failed",
                "x", StringUtils.uncapitalize("X"));

        // Examples from uncapitalize Javadoc
        assertEquals("cat", StringUtils.uncapitalize("cat"));
        assertEquals("cat", StringUtils.uncapitalize("Cat"));
        assertEquals("cAT", StringUtils.uncapitalize("CAT"));
    }

    @Test
    public void testReCapitalize() {
        // reflection type of tests: Sentences.
        assertEquals("uncapitalize(capitalize(String)) failed",
                SENTENCE_UNCAP, StringUtils.uncapitalize(StringUtils.capitalize(SENTENCE_UNCAP)));
        assertEquals("capitalize(uncapitalize(String)) failed",
                SENTENCE_CAP, StringUtils.capitalize(StringUtils.uncapitalize(SENTENCE_CAP)));

        // reflection type of tests: One word.
        assertEquals("uncapitalize(capitalize(String)) failed",
                FOO_UNCAP, StringUtils.uncapitalize(StringUtils.capitalize(FOO_UNCAP)));
        assertEquals("capitalize(uncapitalize(String)) failed",
                FOO_CAP, StringUtils.capitalize(StringUtils.uncapitalize(FOO_CAP)));
    }
}
