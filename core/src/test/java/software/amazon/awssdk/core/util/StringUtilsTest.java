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

package software.amazon.awssdk.core.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.core.util.StringUtils.UTF8;

import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.Test;

/**
 * Unit tests for the StringUtils class.
 */
public class StringUtilsTest {

    /**
     * Tests that {@link StringUtils#fromByteBuffer(ByteBuffer)} correctly
     * base64 encodes the contents in a ByteBuffer and returns the correct
     * result.
     */
    @Test
    public void testFromByteBuffer() {
        String expectedData = "hello world";
        String expectedEncodedData = "aGVsbG8gd29ybGQ=";

        ByteBuffer byteBuffer = ByteBuffer.wrap(expectedData.getBytes());
        String encodedData = StringUtils.fromByteBuffer(byteBuffer);

        assertThat(encodedData, equalTo(expectedEncodedData));
    }

    /**
     * Tests that we can correctly convert Bytes to strings.
     */
    @Test
    public void testFromByte() {
        assertThat(StringUtils.fromByte(new Byte("123")), equalTo("123"));
        assertThat(StringUtils.fromByte(new Byte("-99")), equalTo("-99"));
    }

    @Test
    public void testUTF8Charset() {
        assertThat(UTF8.displayName(), equalTo("UTF-8"));
    }

    /**
     * @see https://github.com/aws/aws-sdk-java/pull/517
     */
    @Test(timeout = 10 * 1000)
    public void replace_ReplacementStringContainsMatchString_DoesNotCauseInfiniteLoop() {
        assertThat(StringUtils.replace("abc", "a", "aa"), equalTo("aabc"));
    }

    @Test
    public void replace_EmptyReplacementString_RemovesAllOccurencesOfMatchString() {
        assertThat(StringUtils.replace("ababab", "a", ""), equalTo("bbb"));
    }

    @Test
    public void replace_MatchNotFound_ReturnsOriginalString() {
        assertThat(StringUtils.replace("abc", "d", "e"), equalTo("abc"));
    }

    @Test
    public void lowerCase_NonEmptyString() {
        String input = "x-amz-InvocAtion-typE";
        String expected = "x-amz-invocation-type";
        assertThat(StringUtils.lowerCase(input), equalTo(expected));
    }

    @Test
    public void lowerCase_NullString() {
        assertThat(StringUtils.lowerCase(null), is(nullValue()));
    }

    @Test
    public void lowerCase_EmptyString() {
        assertThat(StringUtils.lowerCase(""), isEmptyString());
    }

    @Test
    public void upperCase_NonEmptyString() {
        String input = "dHkdjj139_)(e";
        String expected = "DHKDJJ139_)(E";
        assertThat(StringUtils.upperCase(input), equalTo(expected));
    }

    @Test
    public void upperCase_NullString() {
        assertThat(StringUtils.upperCase(null), is(nullValue()));
    }

    @Test
    public void upperCase_EmptyString() {
        assertThat(StringUtils.upperCase(""), isEmptyString());
    }

    @Test
    public void testCompare() {
        assertThat(StringUtils.compare("truck", "Car"), is(greaterThan(0)));
        assertThat(StringUtils.compare("", "dd"), is(lessThan(0)));
        assertThat(StringUtils.compare("dd", ""), is(greaterThan(0)));
        assertThat(StringUtils.compare("", ""), equalTo(0));
        assertThat(StringUtils.compare(" ", ""), is(greaterThan(0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompare_String1Null() {
        StringUtils.compare(null, "test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCompare_String2Null() {
        StringUtils.compare("test", null);
    }

    @Test
    public void testAppendAndCompact() {
        String[] pieces = {" ", "\t", "\n", "\u000b", "\r", "\f", "word", "foo", "bar", "baq"};
        int ITERATIONS = 10000;
        Random rng = new Random();

        for (int i = 0; i < ITERATIONS; i++) {
            int parts = rng.nextInt(10);
            String s = "";
            for (int j = 0; j < parts; j++) {
                s = s + pieces[rng.nextInt(pieces.length)];
            }

            StringBuilder sb = new StringBuilder();
            StringUtils.appendCompactedString(sb, s);
            String compacted = s.replaceAll("\\s+", " ");
            assertThat('[' + compacted + ']', sb.toString(), equalTo(compacted));
        }
    }

    @Test
    public void begins_with_ignore_case() {
        assertThat(StringUtils.beginsWithIgnoreCase("foobar", "FoO"), is(true));
    }

    @Test
    public void begins_with_ignore_case_returns_false_when_seq_doesnot_match() {
        assertThat(StringUtils.beginsWithIgnoreCase("foobar", "baz"), is(false));
    }

    @Test
    public void hasValue() {
        assertThat(StringUtils.hasValue("something"), is(true));
        assertThat(StringUtils.hasValue(null), is(false));
        assertThat(StringUtils.hasValue(""), is(false));
    }

    @Test
    public void isNotBlank() {
        assertThat(StringUtils.isNotBlank("hello"), is(true));
        assertThat(StringUtils.isNotBlank(null), is(false));
        assertThat(StringUtils.isNotBlank(""), is(false));
        assertThat(StringUtils.isNotBlank("\n\t"), is(false));
        assertThat(StringUtils.isNotBlank(" "), is(false));
    }
}
