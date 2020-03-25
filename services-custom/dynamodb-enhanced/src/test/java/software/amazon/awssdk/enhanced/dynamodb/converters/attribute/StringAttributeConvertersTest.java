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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromBoolean;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromBytes;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromListOfAttributeValues;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromMap;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromNumber;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromSetOfBytes;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromSetOfNumbers;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromSetOfStrings;
import static software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue.fromString;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharSequenceAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.CharacterAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.PeriodAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringBufferAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.StringBuilderAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UriAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UrlAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.UuidAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZoneIdAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ZoneOffsetAttributeConverter;
import software.amazon.awssdk.utils.ImmutableMap;

public class StringAttributeConvertersTest {
    @Test
    public void charArrayAttributeConverterBehaves() {
        CharacterArrayAttributeConverter converter = CharacterArrayAttributeConverter.create();

        char[] emptyChars = {};
        char[] chars = {'f', 'o', 'o'};
        char[] numChars = {'4', '2'};

        assertThat(transformFrom(converter, chars).s()).isEqualTo("foo");
        assertThat(transformFrom(converter, emptyChars).s()).isEqualTo("");

        assertThat(transformTo(converter, fromString(""))).isEqualTo(emptyChars);
        assertThat(transformTo(converter, fromString("foo"))).isEqualTo(chars);
        assertThat(transformTo(converter, fromNumber("42"))).isEqualTo(numChars);
    }

    @Test
    public void characterAttributeConverterBehaves() {
        CharacterAttributeConverter converter = CharacterAttributeConverter.create();

        assertThat(transformFrom(converter, 'a').s()).isEqualTo("a");

        assertFails(() -> transformTo(converter, fromString("")));
        assertFails(() -> transformTo(converter, fromString("ab")));

        assertThat(transformTo(converter, fromString("a"))).isEqualTo('a');
    }

    @Test
    public void charSequenceAttributeConverterBehaves() {
        CharSequenceAttributeConverter converter = CharSequenceAttributeConverter.create();

        CharSequence emptyChars = "";
        CharSequence chars = "foo";
        CharSequence numChars = "42";

        assertThat(transformFrom(converter, chars).s()).isEqualTo("foo");
        assertThat(transformFrom(converter, emptyChars).s()).isEqualTo("");

        assertThat(transformTo(converter, fromString(""))).isEqualTo(emptyChars);
        assertThat(transformTo(converter, fromString("foo"))).isEqualTo(chars);
        assertThat(transformTo(converter, fromNumber("42"))).isEqualTo(numChars);
    }

    @Test
    public void periodAttributeConverterBehaves() {
        PeriodAttributeConverter converter = PeriodAttributeConverter.create();

        assertThat(transformFrom(converter, Period.ofYears(-5)).s()).isEqualTo("P-5Y");
        assertThat(transformFrom(converter, Period.ofDays(-1)).s()).isEqualTo("P-1D");
        assertThat(transformFrom(converter, Period.ZERO).s()).isEqualTo("P0D");
        assertThat(transformFrom(converter, Period.ofDays(1)).s()).isEqualTo("P1D");
        assertThat(transformFrom(converter, Period.ofYears(5)).s()).isEqualTo("P5Y");

        assertFails(() -> transformTo(converter, fromString("")));
        assertFails(() -> transformTo(converter, fromString("P")));

        assertThat(transformTo(converter, fromString("P-5Y"))).isEqualTo(Period.ofYears(-5));
        assertThat(transformTo(converter, fromString("P-1D"))).isEqualTo(Period.ofDays(-1));
        assertThat(transformTo(converter, fromString("P0D"))).isEqualTo(Period.ZERO);
        assertThat(transformTo(converter, fromString("P1D"))).isEqualTo(Period.ofDays(1));
        assertThat(transformTo(converter, fromString("P5Y"))).isEqualTo(Period.ofYears(5));
    }

    @Test
    public void stringAttributeConverterBehaves() {
        StringAttributeConverter converter = StringAttributeConverter.create();

        String emptyChars = "";
        String chars = "foo";
        String numChars = "42";

        assertThat(transformFrom(converter, chars).s()).isSameAs(chars);
        assertThat(transformFrom(converter, emptyChars).s()).isSameAs(emptyChars);

        assertThat(transformTo(converter, fromString(emptyChars))).isSameAs(emptyChars);
        assertThat(transformTo(converter, fromString(chars))).isSameAs(chars);
        assertThat(transformTo(converter, fromNumber(emptyChars))).isSameAs(emptyChars);
        assertThat(transformTo(converter, fromNumber(numChars))).isSameAs(numChars);
        assertThat(transformTo(converter, fromBytes(SdkBytes.fromUtf8String("foo")))).isEqualTo("Zm9v");
        assertThat(transformTo(converter, fromBoolean(true))).isEqualTo("true");
        assertThat(transformTo(converter, fromBoolean(false))).isEqualTo("false");
        assertThat(transformTo(converter, fromMap(ImmutableMap.of("a", fromString("b").toAttributeValue(),
                                                                  "c", fromBytes(SdkBytes.fromUtf8String("d")).toAttributeValue()))))
                .isEqualTo("{a=b, c=ZA==}");
        assertThat(transformTo(converter, fromListOfAttributeValues(fromString("a").toAttributeValue(),
                                                                    fromBytes(SdkBytes.fromUtf8String("d")).toAttributeValue())))
                .isEqualTo("[a, ZA==]");
        assertThat(transformTo(converter, fromSetOfStrings("a", "b"))).isEqualTo("[a, b]");
        assertThat(transformTo(converter, fromSetOfBytes(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b"))))
                .isEqualTo("[YQ==,Yg==]");
        assertThat(transformTo(converter, fromSetOfNumbers("1", "2"))).isEqualTo("[1, 2]");
    }

    @Test
    public void stringBuilderAttributeConverterBehaves() {
        StringBuilderAttributeConverter converter = StringBuilderAttributeConverter.create();

        assertThat(transformFrom(converter, new StringBuilder()).s()).isEqualTo("");
        assertThat(transformFrom(converter, new StringBuilder("foo")).s()).isEqualTo("foo");
        assertThat(transformFrom(converter, new StringBuilder("42")).s()).isEqualTo("42");

        assertThat(transformTo(converter, fromString("")).toString()).isEqualTo("");
        assertThat(transformTo(converter, fromString("foo")).toString()).isEqualTo("foo");
        assertThat(transformTo(converter, fromNumber("42")).toString()).isEqualTo("42");
    }

    @Test
    public void stringBufferAttributeConverterBehaves() {
        StringBufferAttributeConverter converter = StringBufferAttributeConverter.create();

        assertThat(transformFrom(converter, new StringBuffer()).s()).isEqualTo("");
        assertThat(transformFrom(converter, new StringBuffer("foo")).s()).isEqualTo("foo");
        assertThat(transformFrom(converter, new StringBuffer("42")).s()).isEqualTo("42");

        assertThat(transformTo(converter, fromString("")).toString()).isEqualTo("");
        assertThat(transformTo(converter, fromString("foo")).toString()).isEqualTo("foo");
        assertThat(transformTo(converter, fromNumber("42")).toString()).isEqualTo("42");
    }

    @Test
    public void uriAttributeConverterBehaves() {
        UriAttributeConverter converter = UriAttributeConverter.create();

        assertThat(transformFrom(converter, URI.create("http://example.com/languages/java/")).s())
                .isEqualTo("http://example.com/languages/java/");
        assertThat(transformFrom(converter, URI.create("sample/a/index.html#28")).s())
                .isEqualTo("sample/a/index.html#28");
        assertThat(transformFrom(converter, URI.create("../../demo/b/index.html")).s())
                .isEqualTo("../../demo/b/index.html");
        assertThat(transformFrom(converter, URI.create("file:///~/calendar")).s()).isEqualTo("file:///~/calendar");

        assertThat(transformTo(converter, fromString("http://example.com/languages/java/")))
                .isEqualTo(URI.create("http://example.com/languages/java/"));
        assertThat(transformTo(converter, fromString("sample/a/index.html#28")))
                .isEqualTo(URI.create("sample/a/index.html#28"));
        assertThat(transformTo(converter, fromString("../../demo/b/index.html")))
                .isEqualTo(URI.create("../../demo/b/index.html"));
        assertThat(transformTo(converter, fromString("file:///~/calendar")))
                .isEqualTo(URI.create("file:///~/calendar"));
    }

    @Test
    public void urlAttributeConverterBehaves() throws MalformedURLException {
        UrlAttributeConverter converter = UrlAttributeConverter.create();

        assertThat(transformFrom(converter, new URL("http://example.com/languages/java/")).s())
                .isEqualTo("http://example.com/languages/java/");
        assertThat(transformTo(converter, fromString("http://example.com/languages/java/")))
                .isEqualTo(new URL("http://example.com/languages/java/"));
    }

    @Test
    public void uuidAttributeConverterBehaves() {
        UuidAttributeConverter converter = UuidAttributeConverter.create();
        UUID uuid = UUID.randomUUID();
        assertThat(transformFrom(converter, uuid).s()).isEqualTo(uuid.toString());
        assertThat(transformTo(converter, fromString(uuid.toString()))).isEqualTo(uuid);
    }

    @Test
    public void zoneIdAttributeConverterBehaves() {
        ZoneIdAttributeConverter converter = ZoneIdAttributeConverter.create();
        assertThat(transformFrom(converter, ZoneId.of("UTC")).s()).isEqualTo("UTC");
        assertFails(() -> transformTo(converter, fromString("XXXXXX")));
        assertThat(transformTo(converter, fromString("UTC"))).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    public void zoneOffsetAttributeConverterBehaves() {
        ZoneOffsetAttributeConverter converter = ZoneOffsetAttributeConverter.create();
        assertThat(transformFrom(converter, ZoneOffset.ofHoursMinutesSeconds(0, -1, -2)).s()).isEqualTo("-00:01:02");
        assertThat(transformFrom(converter, ZoneOffset.ofHoursMinutesSeconds(0, 1, 2)).s()).isEqualTo("+00:01:02");
        assertFails(() -> transformTo(converter, fromString("+99999:00:00")));
        assertThat(transformTo(converter, fromString("-00:01:02")))
                .isEqualTo(ZoneOffset.ofHoursMinutesSeconds(0, -1, -2));
        assertThat(transformTo(converter, fromString("+00:01:02")))
                .isEqualTo(ZoneOffset.ofHoursMinutesSeconds(0, 1, 2));
    }

    @Test
    public void stringSetAttributeConverter_ReturnsSSType() {
        SetAttributeConverter<Set<String>> converter = SetAttributeConverter.setConverter(StringAttributeConverter.create());
        assertThat(converter.attributeValueType()).isEqualTo(AttributeValueType.SS);
    }
}
