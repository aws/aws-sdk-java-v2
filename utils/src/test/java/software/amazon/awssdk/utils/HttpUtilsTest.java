/*
 * Copyright (c) 2017. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.HttpUtils.createUrl;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class HttpUtilsTest {

    private static final URI BASE_URI = invokeSafely(() -> new URI("https://example.com/"));
    private static final URI BASE_URI_WITHOUT_TRAILING_SLASH = invokeSafely(() -> new URI("https://example.com"));

    @Test
    public void baseUriCannotBeNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> createUrl(null, null, null));
    }

    @Test
    public void canCreateBasicUrl() {
        URI uri = HttpUtils.createUrl(BASE_URI, null, null);

        assertThat(uri).hasScheme("https")
                       .hasHost("example.com")
                       .hasPath("/");
    }

    @Test
    public void canHandlePath() {
        URI uri = createUrl(BASE_URI, "/hello/world", null);

        assertThat(uri).hasScheme("https")
                       .hasHost("example.com")
                       .hasPath("/hello/world");
    }

    @Test
    public void canHandleAddingSlashes() {
        URI uri = createUrl(BASE_URI_WITHOUT_TRAILING_SLASH, "hello/world", null);

        assertThat(uri).hasScheme("https")
                       .hasHost("example.com")
                       .hasPath("/hello/world");
    }

    @Test
    public void canOptionallyEscapeDoubleSlashesInThePath() {
        URI uri = createUrl(BASE_URI, "/hello//world", null, true);

        assertThat(uri).hasScheme("https").hasHost("example.com");
        assertThat(uri.getRawPath()).isEqualTo("/hello/%2Fworld");
    }

    @Test
    public void canHandleSingletonQueryParameters() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("key", singletonList("value"));

        URI uri = createUrl(BASE_URI, null, params);

        assertThat(uri).hasQuery("key=value");
    }

    @Test
    public void canHandleFlatteningQueryParameters() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("key", Arrays.asList("foo", "bar"));

        URI uri = createUrl(BASE_URI, null, params);

        assertThat(uri.getRawQuery()).isEqualTo("key=foo&key=bar");

    }

    @Test
    public void canHandleKeyWithoutValueInQueryParameter() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("key", null);
        params.put("anotherKey", emptyList());
        params.put("foo", singletonList("bar"));

        URI uri = createUrl(BASE_URI, null, params);

        assertThat(uri.getRawQuery()).isEqualTo("key&anotherKey&foo=bar");
    }

    @Test
    public void appropriatelyEncodesParameterKeysAndValues() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("key", singletonList("Über"));
        params.put("<key>", emptyList());
        params.put("foo", singletonList("$%&#?"));

        URI uri = createUrl(BASE_URI, null, params);

        assertThat(uri.getRawQuery()).isEqualTo("key=%C3%9Cber&%3Ckey%3E&foo=%24%25%26%23%3F");
    }
}