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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.utils.cache.lru.LruCache;
import software.amazon.awssdk.utils.uri.SdkURI;
import software.amazon.awssdk.utils.uri.internal.UriConstructorArgs;

class SdkURITest {

    @AfterEach
    void resetCache() throws IllegalAccessException {
        Field cacheField = getCacheField();
        cacheField.setAccessible(true);
        cacheField.set(SdkURI.getInstance(), LruCache.builder(UriConstructorArgs::newInstance)
                                                     .maxSize(100)
                                                     .build());
    }

    @Test
    void multipleCreate_withSameStringConstructor_shouldCacheOnlyOnce() {
        String strURI = "https://123456789012.ddb.us-east-1.amazonaws.com";
        URI uri = SdkURI.getInstance().create(strURI);
        assertThat(uri).hasHost("123456789012.ddb.us-east-1.amazonaws.com")
                       .hasNoParameters()
                       .hasNoPort()
                       .hasNoQuery();
        assertThat(getCache().size()).isEqualTo(1);
        URI uri2 = SdkURI.getInstance().create(strURI);
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
            URI u = SdkURI.getInstance().create(uri);
        }
        assertThat(getCache().size()).isEqualTo(8);
    }

    @Test
    void notCached_shouldCreateNewInstance() {
        String strURI = "https://ddb.us-east-1.amazonaws.com";
        URI uri = SdkURI.getInstance().create(strURI);
        assertThat(uri).hasHost("ddb.us-east-1.amazonaws.com")
                       .hasNoParameters()
                       .hasNoPort()
                       .hasNoQuery();
        assertThat(getCache().size()).isEqualTo(0);
        URI uri2 = SdkURI.getInstance().create(strURI);
        assertThat(getCache().size()).isEqualTo(0);
        assertThat(uri).isNotSameAs(uri2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"potatoes tomatoes", "123412341234. potatoes tomatoes"})
    void malformedURI_shouldThrowsSameExceptionAsUriClass(String malformedURI) {
        String malformedUri = "potatoes tomatoes"; // not cached by SdkURI

        assertThatThrownBy(() -> SdkURI.getInstance().create(malformedUri))
            .as("Malformed uri should throw IllegalArgumentException using the create method")
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);

        assertThatThrownBy(() -> SdkURI.getInstance().newURI(malformedUri))
            .as("Malformed uri should throw URISyntaxException using the newURI method")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);

        assertThatThrownBy(() -> SdkURI.getInstance().newURI("scheme", malformedUri, "path", "query", "fragment"))
            .as("Malformed uri should throw URISyntaxException using the newURI with authority method")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);

        assertThatThrownBy(() -> SdkURI.getInstance().newURI("scheme", "userInfo", malformedUri,
                                                               444, "path", "query", "fragment"))
            .as("Malformed uri should throw URISyntaxException using the newURI with host method")
            .isInstanceOf(URISyntaxException.class);
        assertThat(getCache().size()).as("Cache should be empty if create URI fails")
                                     .isEqualTo(0);
    }

    private LruCache<UriConstructorArgs, URI> getCache() {
        Field field = getCacheField();
        field.setAccessible(true);
        try {
            return (LruCache<UriConstructorArgs, URI>) field.get(SdkURI.getInstance());
        } catch (IllegalAccessException e) {
            fail(e);
            return null;
        }
    }

    private Field getCacheField() {
        return ReflectionUtils.streamFields(SdkURI.class,
                                            f -> "cache".equals(f.getName()),
                                            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                              .findFirst()
                              .orElseThrow(() -> new AssertionFailedError("Unexpected error - Could not find field "
                                                                          + "'cache' in " + SdkURI.class.getName()));
    }

}
