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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.utils.cache.bounded.BoundedCache;
import software.amazon.awssdk.utils.uri.SdkUri;
import software.amazon.awssdk.utils.uri.internal.UriConstructorArgs;

class SdkUriTest {

    @AfterEach
    void resetCache() throws IllegalAccessException {
        Field cacheField = getCacheField();
        cacheField.setAccessible(true);
        cacheField.set(SdkUri.getInstance(), BoundedCache.builder(UriConstructorArgs::newInstance)
                                                         .maxSize(100)
                                                         .evictionBatchSize(5)
                                                         .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://123456789012.ddb.us-east-1.amazonaws.com",
                            "http://123456789012.ddb.us-east-1.amazonaws.com"})
    void multipleCreate_simpleURI_SameStringConstructor_ShouldCacheOnlyOnce(String strURI) {
        URI uri = SdkUri.getInstance().create(strURI);
        String scheme = strURI.startsWith("https") ? "https" : "http";
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasScheme(scheme)
                       .hasNoParameters()
                       .hasNoPort()
                       .hasNoQuery();
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().create(strURI);
        assertThat(getCache().size()).isEqualTo(1);
        assertThat(uri).isSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleCreate_FullUri_SameConstructor_ShouldCacheOnlyOne(String scheme) {
        String strURI = scheme + "://123456789012.ddb.us-east-1.amazonaws.com:322/some/path?foo=bar#test";
        URI uri = SdkUri.getInstance().create(strURI);
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasScheme(scheme)
                       .hasNoUserInfo()
                       .hasPort(322)
                       .hasPath("/some/path")
                       .hasQuery("foo=bar")
                       .hasFragment("test");

        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().create(strURI);
        assertThat(getCache().size()).isEqualTo(1);
        assertThat(uri).isSameAs(uri2);

    }

    @Test
    void multipleCreate_withDifferentStringConstructor_shouldCacheOnlyOnce() {
        String[] strURIs = {
            "https://123456789012.ddb.us-east-1.amazonaws.com",
            "https://123456789013.ddb.us-east-1.amazonaws.com",
            "https://123456789014.ddb.us-east-1.amazonaws.com",
            "https://123456789015.ddb.us-east-1.amazonaws.com",
            "https://123456789016.ddb.us-east-1.amazonaws.com",
            "https://123456789017.ddb.us-east-1.amazonaws.com",
            "https://123456789018.ddb.us-east-1.amazonaws.com",
            "https://123456789019.ddb.us-east-1.amazonaws.com",
            };
        for (String uri : strURIs) {
            URI u = SdkUri.getInstance().create(uri);
        }
        assertThat(getCache().size()).isEqualTo(8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleNewUriWithNulls_SameAuthorityConstructor_ShouldCacheOnlyOnce(String scheme) throws URISyntaxException {
        String strURI = "123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().newUri(scheme, strURI, null, null, null);
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasScheme(scheme)
                       .hasNoParameters()
                       .hasNoPort()
                       .hasNoQuery();
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().newUri(scheme, strURI, null, null, null);
        assertThat(getCache().size()).isEqualTo(1);
        assertThat(uri).isSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleNewUri_SameAuthorityConstructor_ShouldCacheOnlyOnce(String scheme) throws URISyntaxException {
        String strURI = "123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().newUri(scheme, strURI, "/somePath/to/resource", "foo=bar", "test");
        assertThat(uri).hasHost(strURI)
                       .hasPath("/somePath/to/resource")
                       .hasQuery("foo=bar")
                       .hasFragment("test")
                       .hasScheme(scheme);
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().newUri(scheme, strURI, "/somePath/to/resource", "foo=bar", "test");
        assertThat(getCache().size()).isEqualTo(1);
        assertThat(uri).isSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleNewUri_DifferentAuthorityConstructor_ShouldCacheAll(String scheme) throws URISyntaxException {
        String strURI = "123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().newUri(scheme, strURI, "/somePath/to/resource", "foo=bar", "test");
        assertThat(uri).hasHost(strURI)
                       .hasPath("/somePath/to/resource")
                       .hasQuery("foo=bar")
                       .hasFragment("test")
                       .hasScheme(scheme);
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().newUri(scheme, strURI, "/some/otherPath/to/resource", null, "test2");
        assertThat(getCache().size()).isEqualTo(2);
        assertThat(uri).isNotSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleNewUriWithNulls_SameHostConstructor_ShouldCacheOnlyOnce(String scheme) throws URISyntaxException {
        String strURI = "123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().newUri(scheme, null, strURI, 322, null, null, null);
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasNoParameters()
                       .hasPort(322)
                       .hasNoQuery();
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().newUri(scheme, null, strURI, 322, null, null, null);
        assertThat(getCache().size()).isEqualTo(1);
        assertThat(uri).isSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleNewUri_SameHostConstructor_ShouldCacheOnlyOnce(String scheme) throws URISyntaxException {
        String strURI = "123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().newUri(scheme, "user1", strURI, 322, "/some/path", "foo=bar", "test");
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasScheme(scheme)
                       .hasUserInfo("user1")
                       .hasPort(322)
                       .hasPath("/some/path")
                       .hasQuery("foo=bar")
                       .hasFragment("test");
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().newUri(scheme, "user1", strURI, 322, "/some/path", "foo=bar", "test");
        assertThat(getCache().size()).isEqualTo(1);
        assertThat(uri).isSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "https"})
    void multipleNewUri_DifferentHostConstructor_ShouldCacheOnlyOnce(String scheme) throws URISyntaxException {
        String strURI = "123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().newUri(scheme, "user1", strURI, 322, "/some/path", "foo=bar", "test");
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasScheme(scheme)
                       .hasUserInfo("user1")
                       .hasPort(322)
                       .hasPath("/some/path")
                       .hasQuery("foo=bar")
                       .hasFragment("test");
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkUri.getInstance().newUri(scheme, "user1", strURI, 322, "/some/other/path", "foo=bar", "test2");
        assertThat(getCache().size()).isEqualTo(2);
        assertThat(uri).isNotSameAs(uri2);
    }

    @Test
    void notCached_shouldCreateNewInstance() {
        String strURI = "https://ddb.us-east-1.amazonaws.com";
        URI uri = SdkUri.getInstance().create(strURI);
        assertThat(uri).hasHost("ddb.us-east-1.amazonaws.com")
                       .hasNoParameters()
                       .hasNoPort()
                       .hasNoQuery();
        assertThat(getCache().size()).isEqualTo(0);
        URI uri2 = SdkUri.getInstance().create(strURI);
        assertThat(getCache().size()).isEqualTo(0);
        assertThat(uri).isNotSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"potatoes tomatoes", "123412341234 potatoes tomatoes"})
    void malformedURI_shouldThrowsSameExceptionAsUriClass(String malformedUri) {

        assertThatThrownBy(() -> SdkUri.getInstance().create(malformedUri))
            .as("Malformed uri should throw IllegalArgumentException using the create method")
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);

        assertThatThrownBy(() -> SdkUri.getInstance().newUri(malformedUri))
            .as("Malformed uri should throw URISyntaxException using the newURI method")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);

        assertThatThrownBy(() -> SdkUri.getInstance().newUri("scheme", malformedUri, "path", "query", "fragment"))
            .as("Malformed uri should throw URISyntaxException using the newURI with authority method")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);

        assertThatThrownBy(() -> new URI("scheme", malformedUri, "path", "query", "fragment"))
            .as("CONSTRUCTOR")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);


        assertThatThrownBy(() -> SdkUri.getInstance().newUri("scheme", "userInfo", malformedUri,
                                                             444, "path", "query", "fragment"))
            .as("Malformed uri should throw URISyntaxException using the newURI with host method")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://123456789.ddb.com",
        "https://123456789.ddb.com",
        "123456789.ddb.com",
        "http://123.ddb.com",
        "https://123.ddb.com",
        "123.ddb.com",
        "http://123z.ddb.com",
        "https://123z.ddb.com",
        "123z.ddb.com",
        "http://1",
        "https://1",
        "1",
        "http://z",
        "https://z",
        "z"
    })
    void shouldNotCache_whenLeadingDigitsDoNotExceedIntegerMaxValue(String strURI) {
        URI uri = SdkUri.getInstance().create(strURI);
        assertThat(getCache().size()).isEqualTo(0);
        URI uri2 = SdkUri.getInstance().create(strURI);
        assertThat(getCache().size()).isEqualTo(0);
        assertThat(uri).isNotSameAs(uri2);
    }


    private BoundedCache<UriConstructorArgs, URI> getCache() {
        Field field = getCacheField();
        field.setAccessible(true);
        try {
            return (BoundedCache<UriConstructorArgs, URI>) field.get(SdkUri.getInstance());
        } catch (IllegalAccessException e) {
            fail(e);
            return null;
        }
    }

    private Field getCacheField() {
        return ReflectionUtils.streamFields(SdkUri.class,
                                            f -> "cache".equals(f.getName()),
                                            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                              .findFirst()
                              .orElseThrow(() -> new AssertionFailedError("Unexpected error - Could not find field "
                                                                          + "'cache' in " + SdkUri.class.getName()));
    }

    @Test
    void create_presignedNonAccountIdUri_traceLogDoesNotContainQuery() {
        List<String> messages = captureTraceLogs(() -> SdkUri.getInstance().create(
            "https://mydb.cluster-abc.us-east-1.rds.amazonaws.com:5432/?DBUser=admin&X-Amz-Signature=SIG"
            + "&X-Amz-Security-Token=TOK"));

        assertNoneContain(messages, "SIG", "TOK", "DBUser", "X-Amz");
        assertThat(messages).contains("skipping cache for uri: "
                                      + "https://mydb.cluster-abc.us-east-1.rds.amazonaws.com:5432/");
    }

    @Test
    void create_presignedAccountIdUri_cacheMissAndHitLogsDoNotContainQueryOrFragment() {
        String uri = "https://123456789012.ddb.us-east-1.amazonaws.com/p?X-Amz-Signature=SIG#frag";
        List<String> messages = captureTraceLogs(() -> {
            SdkUri.getInstance().create(uri);
            SdkUri.getInstance().create(uri);
        });

        assertNoneContain(messages, "SIG", "frag");
        assertThat(messages).contains("Cache empty for https://123456789012.ddb.us-east-1.amazonaws.com/p",
                                      "Using cached uri for https://123456789012.ddb.us-east-1.amazonaws.com/p");
    }

    @Test
    void newUriString_presignedUris_traceLogsDoNotContainQuery() {
        List<String> messages = captureTraceLogs(() -> {
            try {
                SdkUri.getInstance().newUri("https://h.example.com/p?X-Amz-Signature=SIG");
                SdkUri.getInstance().newUri("https://123456789012.ddb.us-east-1.amazonaws.com/p?X-Amz-Signature=SIG");
                SdkUri.getInstance().newUri("https://123456789012.ddb.us-east-1.amazonaws.com/p?X-Amz-Signature=SIG");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        assertNoneContain(messages, "SIG");
        assertThat(messages).contains("skipping cache for uri: https://h.example.com/p",
                                      "Cache empty for https://123456789012.ddb.us-east-1.amazonaws.com/p",
                                      "Using cached uri for https://123456789012.ddb.us-east-1.amazonaws.com/p");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user:secret@host.example.com:443", "user:secret@123456789012.ddb.us-east-1.amazonaws.com:443"})
    void newUriAuthority_withUserInfoAndQuery_traceLogsDoNotContainSecrets(String authority) {
        List<String> messages = captureTraceLogs(() -> {
            try {
                SdkUri.getInstance().newUri("https", authority, "/p", "q=SECRET", null);
                SdkUri.getInstance().newUri("https", authority, "/p", "q=SECRET", null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(messages).isNotEmpty();
        assertNoneContain(messages, "secret", "SECRET");
    }

    @Test
    void newUriAuthority_withUserInfo_logsAuthorityWithoutUserInfo() {
        List<String> messages = captureTraceLogs(() -> {
            try {
                SdkUri.getInstance().newUri("https", "user:pw@host:443", "/p", null, null);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(messages).containsExactly("skipping cache for uri: https://host:443/p");
    }

    @Test
    void newUriHost_withUserInfoAndQuery_logsUriWithoutSecrets() {
        List<String> messages = captureTraceLogs(() -> {
            try {
                SdkUri.getInstance().newUri("https", "user:pw", "host", 443, "/p", "q=SECRET", "frag");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(messages).containsExactly("skipping cache for uri: https://host:443/p");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "https://h:5432/?a=1#f|https://h:5432/",
        "https://h/p#f?x|https://h/p",
        "https://h/p|https://h/p",
        "https://***@h.example.com/a@b?x=1|https://h.example.com/a@b",
        "https://h?x=1|https://h",
        "https://***@h?x=1|https://h",
        "mailto:user@example.com?subject=x|mailto:",
        "h/p@x|h/p@x"
    })
    void create_nonAccountIdUri_logsUriWithoutQueryOrUserInfo(String input, String expected) {
        List<String> messages = captureTraceLogs(() -> SdkUri.getInstance().create(input));

        assertThat(messages).containsExactly("skipping cache for uri: " + expected);
    }

    @Test
    void create_emptyString_logsEmptyUri() {
        List<String> messages = captureTraceLogs(() -> SdkUri.getInstance().create(""));

        assertThat(messages).containsExactly("skipping cache for uri: ");
    }

    private static void assertNoneContain(List<String> messages, String... secrets) {
        for (String message : messages) {
            for (String secret : secrets) {
                assertThat(message).doesNotContain(secret);
            }
        }
    }

    private static List<String> captureTraceLogs(Runnable action) {
        org.apache.logging.log4j.core.Logger logger =
            (org.apache.logging.log4j.core.Logger) LogManager.getLogger(SdkUri.class);
        Level originalLevel = logger.getLevel();
        List<String> messages = new ArrayList<>();
        AbstractAppender appender = new AbstractAppender("SdkUriTestAppender", null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        Configurator.setLevel(SdkUri.class.getName(), Level.TRACE);
        try {
            action.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
            Configurator.setLevel(SdkUri.class.getName(), originalLevel);
        }
        return messages;
    }

    @Test
    void equals_hashCode() {
        EqualsVerifier.forPackage("software.amazon.awssdk.utils.uri")
                      .except(SdkUri.class)
                      .verify();
    }
}