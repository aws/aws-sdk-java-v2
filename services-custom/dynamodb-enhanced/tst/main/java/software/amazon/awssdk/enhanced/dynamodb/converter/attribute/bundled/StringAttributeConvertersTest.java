/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromBoolean;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromBytes;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromListOfAttributeValues;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromMap;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromNumber;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromSetOfBytes;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromSetOfNumbers;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromSetOfStrings;
import static software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue.fromString;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.utils.ImmutableMap;

public class StringAttributeConvertersTest {
    @Test
    public void charArrayAttributeConverterBehaves() {
        CharacterArrayAttributeConverter converter = CharacterArrayAttributeConverter.create();

        char[] emptyChars = {};
        char[] chars = {'f', 'o', 'o'};
        char[] numChars = {'4', '2'};

        assertThat(toAttributeValue(converter, chars).asString()).isEqualTo("foo");
        assertThat(toAttributeValue(converter, emptyChars).asString()).isEqualTo("");

        assertThat(fromAttributeValue(converter, fromString(""))).isEqualTo(emptyChars);
        assertThat(fromAttributeValue(converter, fromString("foo"))).isEqualTo(chars);
        assertThat(fromAttributeValue(converter, fromNumber("42"))).isEqualTo(numChars);
    }

    @Test
    public void characterAttributeConverterBehaves() {
        CharacterAttributeConverter converter = CharacterAttributeConverter.create();

        assertThat(toAttributeValue(converter, 'a').asString()).isEqualTo("a");

        assertFails(() -> fromAttributeValue(converter, fromString("")));
        assertFails(() -> fromAttributeValue(converter, fromString("ab")));

        assertThat(fromAttributeValue(converter, fromString("a"))).isEqualTo('a');
    }

    @Test
    public void charSequenceAttributeConverterBehaves() {
        CharSequenceAttributeConverter converter = CharSequenceAttributeConverter.create();

        CharSequence emptyChars = "";
        CharSequence chars = "foo";
        CharSequence numChars = "42";

        assertThat(toAttributeValue(converter, chars).asString()).isEqualTo("foo");
        assertThat(toAttributeValue(converter, emptyChars).asString()).isEqualTo("");

        assertThat(fromAttributeValue(converter, fromString(""))).isEqualTo(emptyChars);
        assertThat(fromAttributeValue(converter, fromString("foo"))).isEqualTo(chars);
        assertThat(fromAttributeValue(converter, fromNumber("42"))).isEqualTo(numChars);
    }

    @Test
    public void periodAttributeConverterBehaves() {
        PeriodAttributeConverter converter = PeriodAttributeConverter.create();

        assertThat(toAttributeValue(converter, Period.ofYears(-5)).asString()).isEqualTo("P-5Y");
        assertThat(toAttributeValue(converter, Period.ofDays(-1)).asString()).isEqualTo("P-1D");
        assertThat(toAttributeValue(converter, Period.ZERO).asString()).isEqualTo("P0D");
        assertThat(toAttributeValue(converter, Period.ofDays(1)).asString()).isEqualTo("P1D");
        assertThat(toAttributeValue(converter, Period.ofYears(5)).asString()).isEqualTo("P5Y");

        assertFails(() -> fromAttributeValue(converter, fromString("")));
        assertFails(() -> fromAttributeValue(converter, fromString("P")));

        assertThat(fromAttributeValue(converter, fromString("P-5Y"))).isEqualTo(Period.ofYears(-5));
        assertThat(fromAttributeValue(converter, fromString("P-1D"))).isEqualTo(Period.ofDays(-1));
        assertThat(fromAttributeValue(converter, fromString("P0D"))).isEqualTo(Period.ZERO);
        assertThat(fromAttributeValue(converter, fromString("P1D"))).isEqualTo(Period.ofDays(1));
        assertThat(fromAttributeValue(converter, fromString("P5Y"))).isEqualTo(Period.ofYears(5));
    }

    @Test
    public void stringAttributeConverterBehaves() {
        StringAttributeConverter converter = StringAttributeConverter.create();

        String emptyChars = "";
        String chars = "foo";
        String numChars = "42";

        assertThat(toAttributeValue(converter, chars).asString()).isSameAs(chars);
        assertThat(toAttributeValue(converter, emptyChars).asString()).isSameAs(emptyChars);

        assertThat(fromAttributeValue(converter, fromString(emptyChars))).isSameAs(emptyChars);
        assertThat(fromAttributeValue(converter, fromString(chars))).isSameAs(chars);
        assertThat(fromAttributeValue(converter, fromNumber(emptyChars))).isSameAs(emptyChars);
        assertThat(fromAttributeValue(converter, fromNumber(numChars))).isSameAs(numChars);
        assertThat(fromAttributeValue(converter, fromBytes(SdkBytes.fromUtf8String("foo")))).isEqualTo("Zm9v");
        assertThat(fromAttributeValue(converter, fromBoolean(true))).isEqualTo("true");
        assertThat(fromAttributeValue(converter, fromBoolean(false))).isEqualTo("false");
        assertThat(fromAttributeValue(converter, fromMap(ImmutableMap.of("a", fromString("b"),
                                                                         "c", fromBytes(SdkBytes.fromUtf8String("d"))))))
                .isEqualTo("{a=b, c=ZA==}");
        assertThat(fromAttributeValue(converter, fromListOfAttributeValues(fromString("a"),
                                                                           fromBytes(SdkBytes.fromUtf8String("d")))))
                .isEqualTo("[a, ZA==]");
        assertThat(fromAttributeValue(converter, fromSetOfStrings("a", "b"))).isEqualTo("[a, b]");
        assertThat(fromAttributeValue(converter, fromSetOfBytes(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b"))))
                .isEqualTo("[YQ==,Yg==]");
        assertThat(fromAttributeValue(converter, fromSetOfNumbers("1", "2"))).isEqualTo("[1, 2]");
    }

    @Test
    public void stringBuilderAttributeConverterBehaves() {
        StringBuilderAttributeConverter converter = StringBuilderAttributeConverter.create();

        assertThat(toAttributeValue(converter, new StringBuilder()).asString()).isEqualTo("");
        assertThat(toAttributeValue(converter, new StringBuilder("foo")).asString()).isEqualTo("foo");
        assertThat(toAttributeValue(converter, new StringBuilder("42")).asString()).isEqualTo("42");

        assertThat(fromAttributeValue(converter, fromString("")).toString()).isEqualTo("");
        assertThat(fromAttributeValue(converter, fromString("foo")).toString()).isEqualTo("foo");
        assertThat(fromAttributeValue(converter, fromNumber("42")).toString()).isEqualTo("42");
    }

    @Test
    public void stringBufferAttributeConverterBehaves() {
        StringBufferAttributeConverter converter = StringBufferAttributeConverter.create();

        assertThat(toAttributeValue(converter, new StringBuffer()).asString()).isEqualTo("");
        assertThat(toAttributeValue(converter, new StringBuffer("foo")).asString()).isEqualTo("foo");
        assertThat(toAttributeValue(converter, new StringBuffer("42")).asString()).isEqualTo("42");

        assertThat(fromAttributeValue(converter, fromString("")).toString()).isEqualTo("");
        assertThat(fromAttributeValue(converter, fromString("foo")).toString()).isEqualTo("foo");
        assertThat(fromAttributeValue(converter, fromNumber("42")).toString()).isEqualTo("42");
    }

    @Test
    public void uriAttributeConverterBehaves() {
        UriAttributeConverter converter = UriAttributeConverter.create();

        assertThat(toAttributeValue(converter, URI.create("http://example.com/languages/java/")).asString())
                .isEqualTo("http://example.com/languages/java/");
        assertThat(toAttributeValue(converter, URI.create("sample/a/index.html#28")).asString())
                .isEqualTo("sample/a/index.html#28");
        assertThat(toAttributeValue(converter, URI.create("../../demo/b/index.html")).asString())
                .isEqualTo("../../demo/b/index.html");
        assertThat(toAttributeValue(converter, URI.create("file:///~/calendar")).asString()).isEqualTo("file:///~/calendar");

        assertThat(fromAttributeValue(converter, fromString("http://example.com/languages/java/")))
                .isEqualTo(URI.create("http://example.com/languages/java/"));
        assertThat(fromAttributeValue(converter, fromString("sample/a/index.html#28")))
                .isEqualTo(URI.create("sample/a/index.html#28"));
        assertThat(fromAttributeValue(converter, fromString("../../demo/b/index.html")))
                .isEqualTo(URI.create("../../demo/b/index.html"));
        assertThat(fromAttributeValue(converter, fromString("file:///~/calendar")))
                .isEqualTo(URI.create("file:///~/calendar"));
    }

    @Test
    public void urlAttributeConverterBehaves() throws MalformedURLException {
        UrlAttributeConverter converter = UrlAttributeConverter.create();

        assertThat(toAttributeValue(converter, new URL("http://example.com/languages/java/")).asString())
                .isEqualTo("http://example.com/languages/java/");
        assertThat(fromAttributeValue(converter, fromString("http://example.com/languages/java/")))
                .isEqualTo(new URL("http://example.com/languages/java/"));
    }

    @Test
    public void uuidAttributeConverterBehaves() {
        UuidAttributeConverter converter = UuidAttributeConverter.create();
        UUID uuid = UUID.randomUUID();
        assertThat(toAttributeValue(converter, uuid).asString()).isEqualTo(uuid.toString());
        assertThat(fromAttributeValue(converter, fromString(uuid.toString()))).isEqualTo(uuid);
    }

    @Test
    public void zoneIdAttributeConverterBehaves() {
        ZoneIdAttributeConverter converter = ZoneIdAttributeConverter.create();
        assertThat(toAttributeValue(converter, ZoneId.of("UTC")).asString()).isEqualTo("UTC");
        assertFails(() -> fromAttributeValue(converter, fromString("XXXXXX")));
        assertThat(fromAttributeValue(converter, fromString("UTC"))).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    public void zoneOffsetAttributeConverterBehaves() {
        ZoneOffsetAttributeConverter converter = ZoneOffsetAttributeConverter.create();
        assertThat(toAttributeValue(converter, ZoneOffset.ofHoursMinutesSeconds(0, -1, -2)).asString()).isEqualTo("-00:01:02");
        assertThat(toAttributeValue(converter, ZoneOffset.ofHoursMinutesSeconds(0, 1, 2)).asString()).isEqualTo("+00:01:02");
        assertFails(() -> fromAttributeValue(converter, fromString("+99999:00:00")));
        assertThat(fromAttributeValue(converter, fromString("-00:01:02")))
                .isEqualTo(ZoneOffset.ofHoursMinutesSeconds(0, -1, -2));
        assertThat(fromAttributeValue(converter, fromString("+00:01:02")))
                .isEqualTo(ZoneOffset.ofHoursMinutesSeconds(0, 1, 2));
    }
}
