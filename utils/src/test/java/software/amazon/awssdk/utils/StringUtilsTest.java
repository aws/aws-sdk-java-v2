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

package software.amazon.awssdk.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.utils.StringUtils.replacePrefixIgnoreCase;

import org.assertj.core.api.Assertions;
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

    @Test
    public void testStartsWithIgnoreCase() {
        assertTrue(StringUtils.startsWithIgnoreCase("helloworld", "hello"));
        assertTrue(StringUtils.startsWithIgnoreCase("hELlOwOrlD", "hello"));
        assertFalse(StringUtils.startsWithIgnoreCase("hello", "world"));
    }

    @Test
    public void testReplacePrefixIgnoreCase() {
        assertEquals("lloWorld" ,replacePrefixIgnoreCase("helloWorld", "he", ""));
        assertEquals("lloWORld" ,replacePrefixIgnoreCase("helloWORld", "He", ""));
        assertEquals("llOwOrld" ,replacePrefixIgnoreCase("HEllOwOrld", "he", ""));
    }

    @Test
    public void findFirstOccurrence() {
        assertEquals((Character) ':', StringUtils.findFirstOccurrence("abc:def/ghi:jkl/mno", ':', '/'));
        assertEquals((Character) ':', StringUtils.findFirstOccurrence("abc:def/ghi:jkl/mno", '/', ':'));
    }

    @Test
    public void findFirstOccurrence_NoMatch() {
        assertNull(StringUtils.findFirstOccurrence("abc", ':'));
    }

    @Test
    public void safeStringTooBoolean_mixedSpaceTrue_shouldReturnTrue() {
        assertTrue(StringUtils.safeStringToBoolean("TrUe"));
    }

    @Test
    public void safeStringTooBoolean_mixedSpaceFalse_shouldReturnFalse() {
        assertFalse(StringUtils.safeStringToBoolean("fAlSE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeStringTooBoolean_invalidValue_shouldThrowException() {
        assertFalse(StringUtils.safeStringToBoolean("foobar"));
    }
    
    @Test
    public void testRepeat() {
        assertNull(StringUtils.repeat(null, 0));
        assertNull(StringUtils.repeat(null, 1));
        assertNull(StringUtils.repeat(null, 2));
        
        assertEquals("", StringUtils.repeat("", 0));
        assertEquals("", StringUtils.repeat("", 1));
        assertEquals("", StringUtils.repeat("", 2));
        
        assertEquals("", StringUtils.repeat("ab", 0));
        assertEquals("ab", StringUtils.repeat("ab", 1));
        assertEquals("abab", StringUtils.repeat("ab", 2));
        assertEquals("ababab", StringUtils.repeat("ab", 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void repeat_negativeCount_shouldThrowIae() {
        StringUtils.repeat("a", -1);
    }

    @Test(expected = OutOfMemoryError.class)
    public void repeat_maxCount_shouldThrowOom() {
        StringUtils.repeat("a", Integer.MAX_VALUE);
    }

    @Test
    public void replaceEach_javadocExamples() {
        assertNull(StringUtils.replaceEach(null, new String[] {"a"}, new String[] {"b"}));
        assertEquals("", StringUtils.replaceEach("", new String[] {"a"}, new String[] {"b"}));
        assertEquals("aba", StringUtils.replaceEach("aba", new String[0], new String[0]));
        assertEquals("aba", StringUtils.replaceEach("aba", null, null));
        assertEquals("aba", StringUtils.replaceEach("aba", new String[0], null));
        assertEquals("aba", StringUtils.replaceEach("aba", null, new String[0]));
        assertEquals("aba", StringUtils.replaceEach("aba", new String[] {"a"}, null));
        assertEquals("aba", StringUtils.replaceEach("aba", new String[] {null}, new String[] {"a"}));
        assertEquals("aba", StringUtils.replaceEach("aba", new String[] {"a"}, new String[] {null}));
        assertEquals("aba", StringUtils.replaceEach("aba", new String[] {""}, new String[] {"x"}));
        assertEquals("b", StringUtils.replaceEach("aba", new String[] {"a"}, new String[] {""}));
        assertEquals("wcte", StringUtils.replaceEach("abcde", new String[] {"ab", "d"}, new String[] {"w", "t"}));
        assertEquals("dcte", StringUtils.replaceEach("abcde", new String[] {"ab", "d"}, new String[] {"d", "t"}));
        assertEquals("xbx", StringUtils.replaceEach("aba", new String[] {null, "a"}, new String[] {"z", "x"}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceEach_lengthMismatch_throws() {
        StringUtils.replaceEach("abc", new String[] {"a", "b"}, new String[] {"x"});
    }

    @Test
    public void replaceEach_overlappingAndTiedMatches() {
        assertEquals("XY", StringUtils.replaceEach("aaa", new String[] {"aa", "a"}, new String[] {"X", "Y"}));
        assertEquals("22", StringUtils.replaceEach("abab", new String[] {"ba", "ab"}, new String[] {"1", "2"}));
        assertEquals("1c", StringUtils.replaceEach("abc", new String[] {"ab", "a"}, new String[] {"1", "2"}));
        assertEquals("2bc", StringUtils.replaceEach("abc", new String[] {"a", "ab"}, new String[] {"2", "1"}));
        assertEquals("1b", StringUtils.replaceEach("xab", new String[] {"xa", "ab"}, new String[] {"1", "2"}));
    }

    @Test
    public void replaceEach_noMatches_returnsSameInstance() {
        String text = "hello world";
        assertSame(text, StringUtils.replaceEach(text, new String[] {"x", "yz"}, new String[] {"1", "2"}));
    }

    @Test
    public void replaceEach_matchesAtStartAndEnd() {
        assertEquals("1bc2", StringUtils.replaceEach("abcd", new String[] {"a", "d"}, new String[] {"1", "2"}));
        assertEquals("1", StringUtils.replaceEach("abcd", new String[] {"abcd"}, new String[] {"1"}));
        assertEquals("", StringUtils.replaceEach("abcd", new String[] {"abcd"}, new String[] {""}));
    }

    @Test
    public void replaceEach_adjacentMatches() {
        assertEquals("111", StringUtils.replaceEach("aaa", new String[] {"a"}, new String[] {"1"}));
        assertEquals("1212", StringUtils.replaceEach("abab", new String[] {"a", "b"}, new String[] {"1", "2"}));
        assertEquals("2121", StringUtils.replaceEach("baba", new String[] {"a", "b"}, new String[] {"1", "2"}));
    }

    @Test
    public void replaceEach_replacementsAreNotReplacedAgain() {
        assertEquals("ba", StringUtils.replaceEach("ab", new String[] {"a", "b"}, new String[] {"b", "a"}));
        assertEquals("aaaa", StringUtils.replaceEach("aa", new String[] {"a"}, new String[] {"aa"}));
        assertEquals("&amp;lt;", StringUtils.replaceEach("&lt;", new String[] {"&", "<"}, new String[] {"&amp;", "&lt;"}));
    }

    @Test
    public void replaceEach_duplicateSearchStrings_firstOneWins() {
        assertEquals("11", StringUtils.replaceEach("aa", new String[] {"a", "a"}, new String[] {"1", "2"}));
        assertEquals("22", StringUtils.replaceEach("aa", new String[] {"a", "a"}, new String[] {null, "2"}));
    }

    @Test
    public void replaceEach_searchStringConsumedByEarlierMatch_isFoundAgainLater() {
        // "bc" first matches at index 1, but "ab" consumes that "b". The next "bc" is at index 4.
        assertEquals("1c-2", StringUtils.replaceEach("abc-bc", new String[] {"ab", "bc"}, new String[] {"1", "2"}));
        // "a" is cached at index 2, but the long match at index 0 consumes it.
        assertEquals("L-1", StringUtils.replaceEach("xxa-a", new String[] {"a", "xxa"}, new String[] {"1", "L"}));
    }

    @Test
    public void replaceEach_searchStringUnusedForAWhile_isStillReplacedLater() {
        String text = StringUtils.repeat("+", 1000) + "%7E" + StringUtils.repeat("*", 1000) + "%7E+";
        String expected = StringUtils.repeat("%20", 1000) + "~" + StringUtils.repeat("%2A", 1000) + "~%20";
        assertEquals(expected, StringUtils.replaceEach(text, new String[] {"+", "*", "%7E"},
                                                       new String[] {"%20", "%2A", "~"}));
    }

    @Test
    public void replaceEach_xmlEscaping() {
        String[] search = {"&", "\"", "'", "<", ">", "\r", "\n"};
        String[] replace = {"&amp;", "&quot;", "&apos;", "&lt;", "&gt;", "&#x0D;", "&#x0A;"};
        assertEquals("&lt;a href=&quot;x&quot;&gt;Tom &amp; Jerry&apos;s&lt;/a&gt;&#x0D;&#x0A;",
                     StringUtils.replaceEach("<a href=\"x\">Tom & Jerry's</a>\r\n", search, replace));
    }

    @Test
    public void replaceEach_urlEncodingFixups() {
        String[] search = {"+", "*", "%7E", "%2F"};
        String[] replace = {"%20", "%2A", "~", "/"};
        assertEquals("a%20b%2Ac~d/e", StringUtils.replaceEach("a+b*c%7Ed%2Fe", search, replace));
        assertEquals("%20%20~~/%2A", StringUtils.replaceEach("++%7E%7E%2F*", search, replace));
        assertEquals("%7", StringUtils.replaceEach("%7", search, replace));
    }

    @Test
    public void replaceEach_nonAsciiText() {
        assertEquals("\u00e9X\ud83d\ude00Y", StringUtils.replaceEach("\u00e9a\ud83d\ude00b", new String[] {"a", "b"},
                                                                   new String[] {"X", "Y"}));
        assertEquals("smile", StringUtils.replaceEach("\ud83d\ude00", new String[] {"\ud83d\ude00"},
                                                      new String[] {"smile"}));
    }

    @Test
    public void replaceEach_largeInputWithManyMatches_replacesAll() {
        int n = 100_000;
        String text = StringUtils.repeat("&", n) + "<";
        assertEquals(StringUtils.repeat("&amp;", n) + "&lt;",
                     StringUtils.replaceEach(text, new String[] {"&", "<"}, new String[] {"&amp;", "&lt;"}));
    }
}
