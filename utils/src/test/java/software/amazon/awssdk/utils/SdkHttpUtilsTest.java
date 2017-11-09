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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class SdkHttpUtilsTest {
    @Test
    public void urlValuesEncodeCorrectly() {
        String nonEncodedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~";
        String encodedCharactersInput = "\t\n\r !\"#$%&'()*+,/:;<=>?@[\\]^`{|}";
        String encodedCharactersOutput = "%09%0A%0D%20%21%22%23%24%25%26%27%28%29%2A%2B%2C%2F%3A%3B%3C%3D%3E%3F%40%5B%5C%5D%5E%60%7B%7C%7D";

        assertThat(SdkHttpUtils.urlEncode(null)).isEqualTo(null);
        assertThat(SdkHttpUtils.urlEncode("")).isEqualTo("");
        assertThat(SdkHttpUtils.urlEncode(nonEncodedCharacters)).isEqualTo(nonEncodedCharacters);
        assertThat(SdkHttpUtils.urlEncode(encodedCharactersInput)).isEqualTo(encodedCharactersOutput);
    }

    @Test
    public void encodeUrlIgnoreSlashesEncodesCorrectly() {
        String nonEncodedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~/";
        String encodedCharactersInput = "\t\n\r !\"#$%&'()*+,:;<=>?@[\\]^`{|}";
        String encodedCharactersOutput = "%09%0A%0D%20%21%22%23%24%25%26%27%28%29%2A%2B%2C%3A%3B%3C%3D%3E%3F%40%5B%5C%5D%5E%60%7B%7C%7D";

        assertThat(SdkHttpUtils.urlEncodeIgnoreSlashes(null)).isEqualTo(null);
        assertThat(SdkHttpUtils.urlEncodeIgnoreSlashes("")).isEqualTo("");
        assertThat(SdkHttpUtils.urlEncodeIgnoreSlashes(nonEncodedCharacters)).isEqualTo(nonEncodedCharacters);
        assertThat(SdkHttpUtils.urlEncodeIgnoreSlashes(encodedCharactersInput)).isEqualTo(encodedCharactersOutput);
    }

    @Test
    public void formDataValuesEncodeCorrectly() {
        String nonEncodedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.*";
        String encodedCharactersInput = "\t\n\r !\"#$%&'()+,/:;<=>?@[\\]^`{|}~";
        String encodedCharactersOutput = "%09%0A%0D+%21%22%23%24%25%26%27%28%29%2B%2C%2F%3A%3B%3C%3D%3E%3F%40%5B%5C%5D%5E%60%7B%7C%7D%7E";

        assertThat(SdkHttpUtils.formDataEncode(null)).isEqualTo(null);
        assertThat(SdkHttpUtils.formDataEncode("")).isEqualTo("");
        assertThat(SdkHttpUtils.formDataEncode(nonEncodedCharacters)).isEqualTo(nonEncodedCharacters);
        assertThat(SdkHttpUtils.formDataEncode(encodedCharactersInput)).isEqualTo(encodedCharactersOutput);
    }

    @Test
    public void encodeFlattenBehavesCorrectly() {
        HashMap<String, List<String>> values = new LinkedHashMap<>();
        values.put("SingleValue", singletonList("Value"));
        values.put("SpaceValue", singletonList(" "));
        values.put("EncodedValue", singletonList("/"));
        values.put("NoValue", null);
        values.put("NullValue", singletonList(null));
        values.put("BlankValue", singletonList(""));
        values.put("MultiValue", asList("Value1", "Value2"));

        String expectedQueryString = "SingleValue=Value&SpaceValue=%20&EncodedValue=%2F&NullValue&BlankValue=&MultiValue=Value1&MultiValue=Value2";
        String expectedFormDataString = "SingleValue=Value&SpaceValue=+&EncodedValue=%2F&NullValue&BlankValue=&MultiValue=Value1&MultiValue=Value2";

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.encodeAndFlattenQueryParameters(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.encodeAndFlattenFormData(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.encodeFormData(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.encodeQueryParameters(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.flattenQueryParameters(null));

        assertThat(SdkHttpUtils.encodeAndFlattenQueryParameters(values)).hasValue(expectedQueryString);
        assertThat(SdkHttpUtils.encodeAndFlattenFormData(values)).hasValue(expectedFormDataString);
        assertThat(SdkHttpUtils.encodeAndFlattenQueryParameters(Collections.emptyMap())).isNotPresent();
        assertThat(SdkHttpUtils.encodeAndFlattenQueryParameters(Collections.emptyMap())).isNotPresent();
    }

    @Test
    public void urisAppendCorrectly() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.appendUri(null, ""));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.appendUri(null, null));
        assertThat(SdkHttpUtils.appendUri("", null)).isEqualTo("");
        assertThat(SdkHttpUtils.appendUri("", "")).isEqualTo("");
        assertThat(SdkHttpUtils.appendUri("", "bar")).isEqualTo("/bar");
        assertThat(SdkHttpUtils.appendUri("", "/bar")).isEqualTo("/bar");
        assertThat(SdkHttpUtils.appendUri("", "bar/")).isEqualTo("/bar/");
        assertThat(SdkHttpUtils.appendUri("", "/bar/")).isEqualTo("/bar/");
        assertThat(SdkHttpUtils.appendUri("foo.com", null)).isEqualTo("foo.com");
        assertThat(SdkHttpUtils.appendUri("foo.com", "")).isEqualTo("foo.com");
        assertThat(SdkHttpUtils.appendUri("foo.com/", "")).isEqualTo("foo.com/");
        assertThat(SdkHttpUtils.appendUri("foo.com", "bar")).isEqualTo("foo.com/bar");
        assertThat(SdkHttpUtils.appendUri("foo.com/", "bar")).isEqualTo("foo.com/bar");
        assertThat(SdkHttpUtils.appendUri("foo.com", "/bar")).isEqualTo("foo.com/bar");
        assertThat(SdkHttpUtils.appendUri("foo.com/", "/bar")).isEqualTo("foo.com/bar");
        assertThat(SdkHttpUtils.appendUri("foo.com/", "/bar/")).isEqualTo("foo.com/bar/");
        assertThat(SdkHttpUtils.appendUri("foo.com/", "//bar/")).isEqualTo("foo.com//bar/");
    }

    @Test
    public void standardPortsAreCorrect() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.standardPort(null));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> SdkHttpUtils.standardPort("foo"));

        assertThat(SdkHttpUtils.standardPort("http")).isEqualTo(80);
        assertThat(SdkHttpUtils.standardPort("https")).isEqualTo(443);

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> SdkHttpUtils.isUsingStandardPort(null, 80));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> SdkHttpUtils.isUsingStandardPort("foo", 80));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> SdkHttpUtils.isUsingStandardPort("foo", null));

        assertThat(SdkHttpUtils.isUsingStandardPort("http", null)).isTrue();
        assertThat(SdkHttpUtils.isUsingStandardPort("https", null)).isTrue();
        assertThat(SdkHttpUtils.isUsingStandardPort("http", -1)).isTrue();
        assertThat(SdkHttpUtils.isUsingStandardPort("https", -1)).isTrue();
        assertThat(SdkHttpUtils.isUsingStandardPort("http", 80)).isTrue();
        assertThat(SdkHttpUtils.isUsingStandardPort("http", 8080)).isFalse();
        assertThat(SdkHttpUtils.isUsingStandardPort("https", 443)).isTrue();
        assertThat(SdkHttpUtils.isUsingStandardPort("https", 8443)).isFalse();
    }

    @Test
    public void headerRetrievalWorksCorrectly() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("FOO", asList("bar", "baz"));
        headers.put("foo", singletonList(null));
        headers.put("other", singletonList("foo"));
        headers.put("Foo", singletonList("baz2"));

        assertThat(SdkHttpUtils.allMatchingHeaders(headers, "foo")).containsExactly("bar", "baz", null, "baz2");
        assertThat(SdkHttpUtils.firstMatchingHeader(headers, "foo")).hasValue("bar");
        assertThat(SdkHttpUtils.firstMatchingHeader(headers, "other")).hasValue("foo");
        assertThat(SdkHttpUtils.firstMatchingHeader(headers, null)).isNotPresent();
        assertThat(SdkHttpUtils.firstMatchingHeader(headers, "nothing")).isNotPresent();
    }
}
