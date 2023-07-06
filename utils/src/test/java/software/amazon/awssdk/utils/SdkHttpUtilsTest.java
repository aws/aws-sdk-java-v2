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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

public class SdkHttpUtilsTest {
    private final Map<String, String> savedEnvironmentVariableValues = new HashMap<>();

    private static final List<String> SAVED_ENVIRONMENT_VARIABLES = Arrays.asList("http_proxy",
                                                                                  "https_proxy",
                                                                                  "no_proxy");

    private EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    /**
     * Save the current state of the environment variables we're messing around with in these tests so that we can restore them
     * when we are done.
     */
    @BeforeEach
    public void saveEnvironment() throws Exception {
        for (String variable : SAVED_ENVIRONMENT_VARIABLES) {
            savedEnvironmentVariableValues.put(variable, System.getenv(variable));
        }
    }

    /**
     * Reset the environment variables after each test.
     */
    @AfterEach
    public void restoreEnvironment() throws Exception {
        for (String variable : SAVED_ENVIRONMENT_VARIABLES) {
            String savedValue = savedEnvironmentVariableValues.get(variable);

            if (savedValue == null) {
                ENVIRONMENT_VARIABLE_HELPER.remove(variable);
            } else {
                ENVIRONMENT_VARIABLE_HELPER.set(variable, savedValue);
            }
        }
    }

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

    @Test
    public void headersFromCollectionWorksCorrectly() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("FOO", asList("bar", "baz"));
        headers.put("foo", singletonList(null));
        headers.put("other", singletonList("foo"));
        headers.put("Foo", singletonList("baz2"));

        assertThat(SdkHttpUtils.allMatchingHeadersFromCollection(headers, asList("nothing"))).isEmpty();
        assertThat(SdkHttpUtils.allMatchingHeadersFromCollection(headers, asList("foo")))
            .containsExactlyInAnyOrder("bar", "baz", null, "baz2");
        assertThat(SdkHttpUtils.allMatchingHeadersFromCollection(headers, asList("nothing", "foo")))
            .containsExactlyInAnyOrder("bar", "baz", null, "baz2");
        assertThat(SdkHttpUtils.allMatchingHeadersFromCollection(headers, asList("foo", "nothing")))
            .containsExactlyInAnyOrder("bar", "baz", null, "baz2");
        assertThat(SdkHttpUtils.allMatchingHeadersFromCollection(headers, asList("foo", "other")))
            .containsExactlyInAnyOrder("bar", "baz", null, "foo", "baz2");

        assertThat(SdkHttpUtils.firstMatchingHeaderFromCollection(headers, asList("nothing"))).isEmpty();
        assertThat(SdkHttpUtils.firstMatchingHeaderFromCollection(headers, asList("foo"))).hasValue("bar");
        assertThat(SdkHttpUtils.firstMatchingHeaderFromCollection(headers, asList("nothing", "foo"))).hasValue("bar");
        assertThat(SdkHttpUtils.firstMatchingHeaderFromCollection(headers, asList("foo", "nothing"))).hasValue("bar");
        assertThat(SdkHttpUtils.firstMatchingHeaderFromCollection(headers, asList("foo", "other"))).hasValue("foo");
    }

    @Test
    public void isSingleHeader() {
        assertThat(SdkHttpUtils.isSingleHeader("age")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("authorization")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("content-length")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("content-location")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("content-md5")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("content-range")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("content-type")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("date")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("etag")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("expires")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("from")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("host")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("if-modified-since")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("if-range")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("if-unmodified-since")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("last-modified")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("location")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("max-forwards")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("proxy-authorization")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("range")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("referer")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("retry-after")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("server")).isTrue();
        assertThat(SdkHttpUtils.isSingleHeader("user-agent")).isTrue();

        assertThat(SdkHttpUtils.isSingleHeader("custom")).isFalse();
    }

    @Test
    public void uriParams() throws URISyntaxException {
        URI uri = URI.create("https://github.com/aws/aws-sdk-java-v2/issues/2034?reqParam=1234&oParam=3456&reqParam=5678&noval"
                          + "&decoded%26Part=equals%3Dval");
        Map<String, List<String>> uriParams = SdkHttpUtils.uriParams(uri);
        assertThat(uriParams).contains(entry("reqParam", Arrays.asList("1234", "5678")),
                                       entry("oParam", Collections.singletonList("3456")),
                                       entry("noval", Arrays.asList((String)null)),
                                       entry("decoded&Part", Arrays.asList("equals=val")));
    }

    @Test
    public void canParseHttpUsername() throws MalformedURLException {
        assertThat(SdkHttpUtils.parseUsernameFromUrl(new URL("http://user@localhost:25565")))
            .isEqualTo(Optional.of("user"));
        assertThat(SdkHttpUtils.parseUsernameFromUrl(new URL("https://userTwo:password@localhost")))
            .isEqualTo(Optional.of("userTwo"));
        assertThat(SdkHttpUtils.parseUsernameFromUrl(new URL("http://localhost:25565/"))).isEmpty();
        assertThat(SdkHttpUtils.parseUsernameFromUrl(new URL("http://   :    @localhost:25565/"))).isEmpty();
        assertThat(SdkHttpUtils.parseUsernameFromUrl(null)).isEmpty();
    }

    @Test
    public void canParseHttpPassword() throws MalformedURLException {
        assertThat(SdkHttpUtils.parsePasswordFromUrl(new URL("http://user@localhost:25565"))).isEmpty();
        assertThat(SdkHttpUtils.parsePasswordFromUrl(new URL("https://userTwo:password@localhost")))
            .isEqualTo(Optional.of("password"));
        assertThat(SdkHttpUtils.parsePasswordFromUrl(new URL("http://localhost:25565/"))).isEmpty();
        assertThat(SdkHttpUtils.parsePasswordFromUrl(new URL("http://   :    @localhost:25565/"))).isEmpty();
        assertThat(SdkHttpUtils.parsePasswordFromUrl(null)).isEmpty();
    }

    @Test
    public void loadsCorrectProxyFromScheme() throws Exception {
        ENVIRONMENT_VARIABLE_HELPER.set("http_proxy", "http://localhost:25565");
        ENVIRONMENT_VARIABLE_HELPER.set("https_proxy", "https://localhost:25566");

        assertThat(SdkHttpUtils.fetchProxyFromEnvironment("https"))
            .isEqualTo(Optional.of(new URL("https://localhost:25566")));
        assertThat(SdkHttpUtils.fetchProxyFromEnvironment("http"))
            .isEqualTo(Optional.of(new URL("http://localhost:25565")));
        assertThat(SdkHttpUtils.fetchProxyFromEnvironment(null)).isEmpty();

        ENVIRONMENT_VARIABLE_HELPER.set("https_proxy", "invalid-url");
        assertThat(SdkHttpUtils.fetchProxyFromEnvironment("https")).isEmpty();

        ENVIRONMENT_VARIABLE_HELPER.remove("http_proxy");
        ENVIRONMENT_VARIABLE_HELPER.remove("https_proxy");
    }

    @Test
    public void loadsNoProxyFromEnvironment() throws Exception {
        ENVIRONMENT_VARIABLE_HELPER.set(
            "no_proxy",
            "HTTP://LOCALHOST:25565,internal.example.com,internal.example.com,*.example.com"
        );

        assertThat(SdkHttpUtils.parseNonProxyHostsEnvironment()).contains(
            "http://localhost:25565",
            "internal.example.com",
            ".*?.example.com"
        );

        ENVIRONMENT_VARIABLE_HELPER.remove("no_proxy");
    }

}
